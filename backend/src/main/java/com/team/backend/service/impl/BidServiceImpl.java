package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.AutoBid;
import com.team.backend.entity.BidTransaction;
import com.team.backend.exception.AuctionClosedException;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.service.BidService;
import com.team.backend.service.EventPublisher;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BidServiceImpl - non-transactional entry point
 * - retry loop for optimistic/pessimistic conflicts
 * - fallback to in-memory lock if DB locking fails
 * - delegates transactional core to BidTransactionalService
 *
 * Lưu ý: EventPublisher là tùy chọn; nếu không có bean, sẽ hoạt động bình thường.
 */
@Service
public class BidServiceImpl implements BidService {

    private static final Logger log = LoggerFactory.getLogger(BidServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AutoBidRepository autoBidRepository;
    private final BidTransactionalService bidTransactionalService;
    private final ConcurrentHashMap<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final double minIncrement;
    private final long antiSnipingThresholdSeconds;
    private final long antiSnipingExtendSeconds;
    private final int maxRetries;

    // EventPublisher là tùy chọn; nếu không có bean, sẽ là null
    private final EventPublisher eventPublisher;

    public BidServiceImpl(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          AutoBidRepository autoBidRepository,
                          BidTransactionalService bidTransactionalService,
                          @Value("${auction.bid.min-increment:1.0}") double minIncrement,
                          @Value("${auction.anti-sniping.threshold-seconds:30}") long antiSnipingThresholdSeconds,
                          @Value("${auction.anti-sniping.extend-seconds:60}") long antiSnipingExtendSeconds,
                          @Value("${auction.bid.max-retries:3}") int maxRetries,
                          EventPublisher eventPublisher) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
        this.bidTransactionalService = bidTransactionalService;
        this.minIncrement = minIncrement;
        this.antiSnipingThresholdSeconds = antiSnipingThresholdSeconds;
        this.antiSnipingExtendSeconds = antiSnipingExtendSeconds;
        this.maxRetries = Math.max(1, maxRetries);
        this.eventPublisher = eventPublisher;
    }

    private ReentrantLock getLock(UUID auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    /**
     * Public entry point for placing a bid.
     * Retries on optimistic/pessimistic lock conflicts and falls back to in-memory lock if needed.
     */
    @Override
    public BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId và bidderId là bắt buộc");
        }
        if (amount <= 0.0) {
            throw new InvalidBidException("Số tiền đặt phải lớn hơn 0");
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // delegate to transactional service (ensures @Transactional proxy is used)
                BidTransaction tx = bidTransactionalService.placeBidTransactionalAttempt(
                        auctionId, bidderId, amount, minIncrement, antiSnipingThresholdSeconds, antiSnipingExtendSeconds, eventPublisher);
                return tx;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict attempt {} for auction {}: {}", attempt, auctionId, ex.getMessage());
                if (attempt >= maxRetries) {
                    throw new InvalidBidException("Xung đột đồng thời, vui lòng thử lại sau");
                }
                backoffSleep(attempt);
            } catch (PessimisticLockingFailureException ex) {
                log.warn("Pessimistic lock failure attempt {} for auction {}: {}", attempt, auctionId, ex.getMessage());
                if (attempt >= maxRetries) {
                    log.info("Falling back to in-memory lock for auction {}", auctionId);
                    return placeBidWithInMemoryLock(auctionId, bidderId, amount);
                }
                backoffSleep(attempt);
            } catch (ResourceNotFoundException | InvalidBidException | AuctionClosedException ex) {
                // domain errors: bubble up immediately
                throw ex;
            } catch (RuntimeException ex) {
                // unexpected runtime error: fallback to in-memory lock
                log.warn("Unexpected error on attempt {} for auction {}: {}, falling back to in-memory lock", attempt, auctionId, ex.getMessage());
                return placeBidWithInMemoryLock(auctionId, bidderId, amount);
            }
        }

        throw new InvalidBidException("Không thể đặt giá, vui lòng thử lại sau");
    }

    private void backoffSleep(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Fallback path using an in-memory ReentrantLock per-auction.
     * Only safe for single-instance deployments or local testing.
     */
    protected BidTransaction placeBidWithInMemoryLock(UUID auctionId, UUID bidderId, double amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

            // validate auction state and times
            validateAuctionForBid(auction);

            double minAllowed = auction.getCurrentPrice() + minIncrement;
            if (amount < minAllowed) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
            }

            // anti-sniping
            extendAuctionIfNeeded(auction);

            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
            BidTransaction saved = bidRepository.save(tx);

            // apply auto-bid synchronously (same instance)
            applyAutoBidIfNeeded(auction, bidderId);

            // publish event after commit is not available here; if eventPublisher exists, call directly (best-effort)
            if (eventPublisher != null) {
                try {
                    eventPublisher.publishBidPlaced(auctionId, bidderId, amount, null, Instant.now());
                } catch (Exception e) {
                    log.warn("EventPublisher failed in fallback path: {}", e.getMessage());
                }
            }

            log.debug("Bid placed (in-memory lock): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Validate auction state/time before accepting a bid.
     * This method is used by fallback path; transactional path validates inside transactional service.
     */
    private void validateAuctionForBid(Auction auction) {
        if (auction == null) {
            throw new ResourceNotFoundException("Auction is null");
        }
        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELLED) {
            throw new AuctionClosedException("Auction đã đóng");
        }
        Instant now = Instant.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction chưa bắt đầu");
        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            // mark finished and persist
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auctionRepository.save(auction);
            throw new AuctionClosedException("Auction đã kết thúc");
        }
    }

    /**
     * Auto-bid logic used by fallback path (synchronous).
     * If you already run auto-bid inside transactional service, this will be a no-op in normal flow.
     */
    private void applyAutoBidIfNeeded(Auction auction, UUID triggeringBidderId) {
        List<AutoBid> autoBids = autoBidRepository
                .findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auction.getId());

        AutoBid best = null;
        double secondBestLimit = auction.getCurrentPrice();

        for (AutoBid autoBid : autoBids) {
            if (autoBid.getBidderId().equals(triggeringBidderId)) {
                continue;
            }
            if (autoBid.getMaxAmount() < auction.getCurrentPrice() + minIncrement) {
                continue;
            }
            if (best == null) {
                best = autoBid;
            } else {
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

        // best-effort publish
        if (eventPublisher != null) {
            try {
                eventPublisher.publishBidPlaced(auction.getId(), best.getBidderId(), autoAmount, null, Instant.now());
            } catch (Exception e) {
                log.warn("EventPublisher failed to publish auto-bid: {}", e.getMessage());
            }
        }
    }

    /**
     * Anti-sniping: extend auction end time if remaining time <= threshold.
     * This method is used by fallback path; transactional path handles it inside transaction.
     */
    private void extendAuctionIfNeeded(Auction auction) {
        Instant now = Instant.now();
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) return;
        long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        if (secondsLeft <= antiSnipingThresholdSeconds) {
            auction.setEndTime(auction.getEndTime().plusSeconds(antiSnipingExtendSeconds));
            log.debug("Anti-sniping: extended auction {} by {} seconds", auction.getId(), antiSnipingExtendSeconds);
        }
    }

    @Override
    public List<BidTransaction> getBidHistory(UUID auctionId) {
        return bidRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId);
    }
}
