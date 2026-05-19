package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.BidTransaction;
import com.team.backend.entity.AutoBid;
import com.team.backend.exception.AuctionClosedException;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * BidTransactionalService - contains the transactional core logic.
 * This class is a separate Spring bean so @Transactional works when called from BidServiceImpl.
 */
@Service
public class BidTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(BidTransactionalService.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AutoBidRepository autoBidRepository;

    @Autowired
    public BidTransactionalService(AuctionRepository auctionRepository,
                                   BidRepository bidRepository,
                                   AutoBidRepository autoBidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
    }

    /**
     * Transactional attempt to place a bid.
     * - loads auction with pessimistic lock (findByIdForUpdate)
     * - validates, applies anti-sniping, updates auction, saves bid
     * - applies auto-bid logic inside same transaction
     * - registers afterCommit event publish via provided eventPublisher (may be null)
     */
    @Transactional
    public BidTransaction placeBidTransactionalAttempt(UUID auctionId,
                                                       UUID bidderId,
                                                       double amount,
                                                       double minIncrement,
                                                       long antiSnipingThresholdSeconds,
                                                       long antiSnipingExtendSeconds,
                                                       BidServiceImpl.EventPublisher eventPublisher) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        // validate
        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELLED) {
            throw new AuctionClosedException("Auction đã đóng");
        }
        Instant now = Instant.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction chưa bắt đầu");
        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auctionRepository.save(auction);
            throw new AuctionClosedException("Auction đã kết thúc");
        }

        double minAllowed = auction.getCurrentPrice() + minIncrement;
        if (amount < minAllowed) {
            throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
        }

        // anti-sniping
        if (auction.getEndTime() != null) {
            long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
            if (secondsLeft <= antiSnipingThresholdSeconds) {
                auction.setEndTime(auction.getEndTime().plusSeconds(antiSnipingExtendSeconds));
                log.debug("Anti-sniping: extended auction {} by {} seconds", auction.getId(), antiSnipingExtendSeconds);
            }
        }

        double previousPrice = auction.getCurrentPrice();
        UUID previousLeader = auction.getLeaderId();

        // apply bid
        auction.setCurrentPrice(amount);
        auction.setLeaderId(bidderId);
        auctionRepository.save(auction);

        BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
        BidTransaction saved = bidRepository.save(tx);

        // apply auto-bid inside same transaction
        applyAutoBidIfNeeded(auction, bidderId, minIncrement);

        // register afterCommit event publish
        if (eventPublisher != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            final UUID aId = auctionId;
            final UUID bId = bidderId;
            final double amt = amount;
            final UUID prevLeader = previousLeader;
            final Instant ts = Instant.now();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        eventPublisher.publishBidPlaced(aId, bId, amt, prevLeader, ts);
                    } catch (Exception e) {
                        log.warn("EventPublisher failed to publish BidPlaced for auction {}: {}", aId, e.getMessage());
                    }
                }
            });
        }

        log.debug("Bid placed (transactional): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
        return saved;
    }

    private void applyAutoBidIfNeeded(Auction auction, UUID triggeringBidderId, double minIncrement) {
        List<AutoBid> autoBids = autoBidRepository
                .findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auction.getId());

        AutoBid best = null;
        double secondBestLimit = auction.getCurrentPrice();

        for (AutoBid autoBid : autoBids) {
            if (autoBid.getBidderId().equals(triggeringBidderId)) continue;
            if (autoBid.getMaxAmount() < auction.getCurrentPrice() + minIncrement) continue;
            if (best == null) best = autoBid;
            else {
                secondBestLimit = Math.max(secondBestLimit, autoBid.getMaxAmount());
                break;
            }
        }

        if (best == null) return;

        double autoAmount = Math.min(best.getMaxAmount(), secondBestLimit + minIncrement);
        if (autoAmount <= auction.getCurrentPrice()) return;

        auction.setCurrentPrice(autoAmount);
        auction.setLeaderId(best.getBidderId());
        auctionRepository.save(auction);

        BidTransaction autoTx = new BidTransaction(auction.getId(), best.getBidderId(), autoAmount, Instant.now());
        bidRepository.save(autoTx);

        // Note: event publishing for auto-bid can be registered similarly if needed
    }
}
