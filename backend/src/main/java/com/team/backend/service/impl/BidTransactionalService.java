package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.AutoBid;
import com.team.backend.entity.BidTransaction;
import com.team.backend.exception.AuctionClosedException;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.WalletRepository;
import com.team.backend.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BidTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(BidTransactionalService.class);
    private static final int MAX_AUTO_BID_ROUNDS = 10;

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AutoBidRepository autoBidRepository;
    private final WalletRepository walletRepository;

    public BidTransactionalService(AuctionRepository auctionRepository,
                                   BidRepository bidRepository,
                                   AutoBidRepository autoBidRepository,
                                   WalletRepository walletRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, noRollbackFor = AuctionClosedException.class)
    public BidTransaction placeBidTransactionalAttempt(UUID auctionId,
                                                       UUID bidderId,
                                                       double amount,
                                                       double minIncrement,
                                                       long antiSnipingThresholdSeconds,
                                                       long antiSnipingExtendSeconds,
                                                       EventPublisher eventPublisher) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        validateAuctionForBidTransactional(auction, bidderId, amount, eventPublisher);

        Instant now = Instant.now();

        double minAllowed = auction.getCurrentPrice() + minIncrement;
        if (amount < minAllowed) {
            throw new InvalidBidException("Bid must be at least " + minAllowed);
        }

        boolean extendedByAntiSniping = false;
        if (auction.getEndTime() != null) {
            long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
            if (secondsLeft <= antiSnipingThresholdSeconds) {
                auction.setEndTime(auction.getEndTime().plusSeconds(antiSnipingExtendSeconds));
                auctionRepository.save(auction);
                extendedByAntiSniping = true;
            }
        }

        UUID previousLeader = auction.getLeaderId();

        auction.setCurrentPrice(amount);
        auction.setLeaderId(bidderId);
        auctionRepository.save(auction);

        BidTransaction savedManualTx = bidRepository.save(new BidTransaction(auctionId, bidderId, amount, Instant.now()));

        boolean changed;
        int round = 0;
        do {
            changed = applyOneRoundAutoBid(auction, minIncrement, eventPublisher);
            round++;
        } while (changed && round < MAX_AUTO_BID_ROUNDS);

        if (eventPublisher != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            final UUID eventAuctionId = auctionId;
            final UUID eventBidderId = bidderId;
            final double eventAmount = amount;
            final UUID eventPreviousLeader = previousLeader;
            final Instant eventTimestamp = Instant.now();
            final boolean eventExtended = extendedByAntiSniping;
            final double currentPriceForEvent = auction.getCurrentPrice();
            final Instant newEndTimeForEvent = auction.getEndTime();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        eventPublisher.publishBidPlaced(eventAuctionId, eventBidderId, eventAmount, eventPreviousLeader, eventTimestamp);
                        if (eventExtended) {
                            eventPublisher.publishAuctionExtended(eventAuctionId, currentPriceForEvent, newEndTimeForEvent);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to publish bid events for auction {}", eventAuctionId, e);
                    }
                }
            });
        }

        return savedManualTx;
    }

    private void validateAuctionForBidTransactional(Auction auction,
                                                    UUID bidderId,
                                                    double amount,
                                                    EventPublisher eventPublisher) {
        if (auction == null) {
            throw new ResourceNotFoundException("Auction is missing");
        }
        if (auction.getState() == null) {
            throw new InvalidBidException("Auction state is missing");
        }
        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELLED) {
            throw new AuctionClosedException("Auction is closed");
        }

        UUID sellerId = auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy();
        if (sellerId != null && sellerId.equals(bidderId)) {
            throw new InvalidBidException("Seller cannot bid on their own auction");
        }

        ensureSufficientBalance(bidderId, amount);

        Instant now = Instant.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction has not started");
        }
        if (auction.getEndTime() != null && !now.isBefore(auction.getEndTime())) {
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auctionRepository.save(auction);
            registerAuctionClosedAfterCommit(auction, eventPublisher);
            throw new AuctionClosedException("Auction has ended");
        }
        if (auction.getState() == AuctionState.SCHEDULED && auction.getStartTime() != null && !now.isBefore(auction.getStartTime())) {
            auction.setState(AuctionState.ACTIVE);
            auctionRepository.save(auction);
        }
        if (auction.getState() != AuctionState.ACTIVE) {
            throw new InvalidBidException("Auction is not active");
        }
    }

    private boolean applyOneRoundAutoBid(Auction auction, double minIncrement, EventPublisher eventPublisher) {
        List<AutoBid> autoBids = autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auction.getId());
        if (autoBids == null || autoBids.isEmpty()) {
            return false;
        }

        UUID currentLeader = auction.getLeaderId();
        AutoBid best = null;
        Double secondBestMax = null;

        for (AutoBid autoBid : autoBids) {
            if (autoBid.getBidderId().equals(currentLeader)) {
                continue;
            }
            double autoBidStep = resolveAutoBidStep(autoBid, minIncrement);
            if (best == null) {
                if (autoBid.getMaxAmount() >= auction.getCurrentPrice() + autoBidStep) {
                    best = autoBid;
                }
            } else {
                secondBestMax = autoBid.getMaxAmount();
                break;
            }
        }

        if (best == null) {
            return false;
        }

        double secondLimit = secondBestMax == null
                ? auction.getCurrentPrice()
                : Math.max(auction.getCurrentPrice(), secondBestMax);
        double bestStep = resolveAutoBidStep(best, minIncrement);
        double autoAmount = Math.min(best.getMaxAmount(), secondLimit + bestStep);

        if (autoAmount <= auction.getCurrentPrice()) {
            return false;
        }

        UUID previousLeader = currentLeader;
        auction.setCurrentPrice(autoAmount);
        auction.setLeaderId(best.getBidderId());
        auctionRepository.save(auction);
        bidRepository.save(new BidTransaction(auction.getId(), best.getBidderId(), autoAmount, Instant.now()));

        if (eventPublisher != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            final UUID eventAuctionId = auction.getId();
            final UUID eventBidderId = best.getBidderId();
            final double eventAmount = autoAmount;
            final UUID eventPreviousLeader = previousLeader;
            final Instant eventTimestamp = Instant.now();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        eventPublisher.publishAutoBidPlaced(eventAuctionId, eventBidderId, eventAmount, eventPreviousLeader, eventTimestamp);
                    } catch (Exception e) {
                        log.warn("Failed to publish auto-bid event for auction {}", eventAuctionId, e);
                    }
                }
            });
        }

        return true;
    }

    private double resolveAutoBidStep(AutoBid autoBid, double minIncrement) {
        if (autoBid == null) {
            return minIncrement;
        }

        return Math.max(minIncrement, autoBid.resolveBidStep(minIncrement));
    }

    private void ensureSufficientBalance(UUID bidderId, double amount) {
        BigDecimal required = BigDecimal.valueOf(amount);
        BigDecimal balance = walletRepository.findByUserId(bidderId)
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        if (balance.compareTo(required) < 0) {
            throw new InvalidBidException("Insufficient wallet balance for this bid");
        }
    }

    private void registerAuctionClosedAfterCommit(Auction auction, EventPublisher eventPublisher) {
        if (eventPublisher == null || auction == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        final UUID eventAuctionId = auction.getId();
        final UUID eventWinnerId = auction.getWinnerId();
        final double eventFinalPrice = auction.getCurrentPrice();
        final Instant eventTimestamp = Instant.now();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    eventPublisher.publishAuctionClosed(eventAuctionId, eventWinnerId, eventFinalPrice, eventTimestamp);
                } catch (Exception e) {
                    log.warn("Failed to publish auction closed event for auction {}", eventAuctionId, e);
                }
            }
        });
    }
}
