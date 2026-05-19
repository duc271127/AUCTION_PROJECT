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
import com.team.backend.service.EventPublisher;
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

@Service
public class BidTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(BidTransactionalService.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AutoBidRepository autoBidRepository;

    private static final int MAX_AUTO_BID_ROUNDS = 10;

    @Autowired
    public BidTransactionalService(AuctionRepository auctionRepository,
                                   BidRepository bidRepository,
                                   AutoBidRepository autoBidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
    }

    @Transactional
    public BidTransaction placeBidTransactionalAttempt(UUID auctionId,
                                                       UUID bidderId,
                                                       double amount,
                                                       double minIncrement,
                                                       long antiSnipingThresholdSeconds,
                                                       long antiSnipingExtendSeconds,
                                                       EventPublisher eventPublisher) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction không tồn tại: " + auctionId));

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

        // Anti-sniping
        boolean extendedByAntiSniping = false;
        if (auction.getEndTime() != null) {
            long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
            if (secondsLeft <= antiSnipingThresholdSeconds) {
                auction.setEndTime(auction.getEndTime().plusSeconds(antiSnipingExtendSeconds));
                auctionRepository.save(auction);
                extendedByAntiSniping = true;
                log.debug("Anti-sniping: kéo dài auction {} thêm {} giây", auction.getId(), antiSnipingExtendSeconds);
            }
        }

        UUID previousLeader = auction.getLeaderId();

        // Apply manual bid
        auction.setCurrentPrice(amount);
        auction.setLeaderId(bidderId);
        auctionRepository.save(auction);

        BidTransaction savedTx = bidRepository.save(new BidTransaction(auctionId, bidderId, amount, Instant.now()));

        // Auto-bid rounds until stable
        boolean changed;
        int round = 0;
        do {
            changed = applyOneRoundAutoBid(auction, minIncrement, eventPublisher);
            round++;
        } while (changed && round < MAX_AUTO_BID_ROUNDS);

        if (changed) {
            log.warn("Auto-bid có thể chưa ổn định sau {} vòng (giới hạn {}) cho auction {}", round, MAX_AUTO_BID_ROUNDS, auction.getId());
        }

        // Register afterCommit events: manual bid and possible extension
        if (eventPublisher != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            final UUID aId = auctionId;
            final UUID bId = bidderId;
            final double amt = amount;
            final UUID prevLeader = previousLeader;
            final Instant ts = Instant.now();
            final boolean extended = extendedByAntiSniping;
            final double currentPriceForEvent = auction.getCurrentPrice();
            final Instant newEndTimeForEvent = auction.getEndTime();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        eventPublisher.publishBidPlaced(aId, bId, amt, prevLeader, ts);
                        if (extended) {
                            eventPublisher.publishAuctionExtended(aId, currentPriceForEvent, newEndTimeForEvent);
                        }
                    } catch (Exception e) {
                        log.warn("EventPublisher thất bại khi publish sự kiện cho auction {}: {}", aId, e.getMessage());
                    }
                }
            });
        }

        log.debug("Đặt giá hoàn tất (transactional): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
        return savedTx;
    }

    /**
     * Một vòng auto-bid. Nếu có auto-bid được áp dụng, sẽ lưu bid và (tuỳ chọn) đăng ký event publish.
     * Trả về true nếu có thay đổi (cần lặp tiếp).
     */
    private boolean applyOneRoundAutoBid(Auction auction, double minIncrement, EventPublisher eventPublisher) {
        List<AutoBid> autoBids = autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auction.getId());
        if (autoBids == null || autoBids.isEmpty()) return false;

        UUID currentLeader = auction.getLeaderId();
        AutoBid best = null;
        Double secondBestMax = null;

        for (AutoBid ab : autoBids) {
            if (ab.getBidderId().equals(currentLeader)) continue;
            if (best == null) {
                if (ab.getMaxAmount() >= auction.getCurrentPrice() + minIncrement) {
                    best = ab;
                } else {
                    continue;
                }
            } else {
                secondBestMax = ab.getMaxAmount();
                break;
            }
        }

        if (best == null) return false;

        double secondLimit = (secondBestMax == null) ? auction.getCurrentPrice() : Math.max(auction.getCurrentPrice(), secondBestMax);
        double autoAmount = Math.min(best.getMaxAmount(), secondLimit + minIncrement);

        if (autoAmount <= auction.getCurrentPrice()) return false;

        // Áp dụng auto-bid
        auction.setCurrentPrice(autoAmount);
        auction.setLeaderId(best.getBidderId());
        auctionRepository.save(auction);

        BidTransaction autoTx = new BidTransaction(auction.getId(), best.getBidderId(), autoAmount, Instant.now());
        bidRepository.save(autoTx);

        log.debug("Auto-bid áp dụng: auction={}, bidder={}, amount={}", auction.getId(), best.getBidderId(), autoAmount);

        // Đăng ký event publish cho auto-bid sau commit nếu có publisher
        if (eventPublisher != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            final UUID aId = auction.getId();
            final UUID bId = best.getBidderId();
            final double amt = autoAmount;
            final Instant ts = Instant.now();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        eventPublisher.publishAutoBidPlaced(aId, bId, amt, ts);
                    } catch (Exception e) {
                        log.warn("EventPublisher thất bại khi publish auto-bid cho auction {}: {}", aId, e.getMessage());
                    }
                }
            });
        }

        return true;
    }
}
