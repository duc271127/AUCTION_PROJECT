package com.team.backend.service.impl;

import com.team.backend.dto.AuctionSummaryResponse;
import com.team.backend.dto.BidHistoryDto;
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
import com.team.backend.service.AuctionHelper;
import com.team.backend.service.BidService;
import com.team.backend.service.EventPublisher;
import com.team.backend.service.bid.BidWalletService;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class BidServiceImpl implements BidService {

    private static final Logger log = LoggerFactory.getLogger(BidServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AutoBidRepository autoBidRepository;
    private final BidTransactionalService bidTransactionalService;
    private final BidWalletService bidWalletService;
    private final AuctionHelper auctionHelper;
    private final ConcurrentHashMap<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final double minIncrement;
    private final long antiSnipingThresholdSeconds;
    private final long antiSnipingExtendSeconds;
    private final int maxRetries;
    private final EventPublisher eventPublisher;

    public BidServiceImpl(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          AutoBidRepository autoBidRepository,
                          BidTransactionalService bidTransactionalService,
                          BidWalletService bidWalletService,
                          double minIncrement,
                          long antiSnipingThresholdSeconds,
                          long antiSnipingExtendSeconds,
                          int maxRetries,
                          Optional<EventPublisher> eventPublisherOptional) {
        this(auctionRepository,
                bidRepository,
                autoBidRepository,
                bidTransactionalService,
                bidWalletService,
                null,
                minIncrement,
                antiSnipingThresholdSeconds,
                antiSnipingExtendSeconds,
                maxRetries,
                eventPublisherOptional);
    }

    @Autowired
    public BidServiceImpl(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          AutoBidRepository autoBidRepository,
                          BidTransactionalService bidTransactionalService,
                          BidWalletService bidWalletService,
                          AuctionHelper auctionHelper,
                          @Value("${auction.bid.min-increment:1.0}") double minIncrement,
                          @Value("${auction.anti-sniping.threshold-seconds:30}") long antiSnipingThresholdSeconds,
                          @Value("${auction.anti-sniping.extend-seconds:60}") long antiSnipingExtendSeconds,
                          @Value("${auction.bid.max-retries:3}") int maxRetries,
                          Optional<EventPublisher> eventPublisherOptional) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
        this.bidTransactionalService = bidTransactionalService;
        this.bidWalletService = bidWalletService;
        this.auctionHelper = auctionHelper;
        this.minIncrement = minIncrement;
        this.antiSnipingThresholdSeconds = antiSnipingThresholdSeconds;
        this.antiSnipingExtendSeconds = antiSnipingExtendSeconds;
        this.maxRetries = Math.max(1, maxRetries);
        this.eventPublisher = eventPublisherOptional.orElse(null);
    }

    @Override
    public BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId and bidderId are required");
        }
        if (amount <= 0.0) {
            throw new InvalidBidException("Bid amount must be greater than 0");
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return bidTransactionalService.placeBidTransactionalAttempt(
                        auctionId,
                        bidderId,
                        amount,
                        minIncrement,
                        antiSnipingThresholdSeconds,
                        antiSnipingExtendSeconds,
                        eventPublisher
                );
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt >= maxRetries) {
                    throw new InvalidBidException("Concurrent bid conflict. Please try again.");
                }
                backoffSleep(attempt);
            } catch (PessimisticLockingFailureException ex) {
                if (attempt >= maxRetries) {
                    return placeBidWithInMemoryLock(auctionId, bidderId, amount);
                }
                backoffSleep(attempt);
            } catch (ResourceNotFoundException | InvalidBidException | AuctionClosedException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.warn("Falling back to in-memory bid lock for auction {}", auctionId, ex);
                return placeBidWithInMemoryLock(auctionId, bidderId, amount);
            }
        }

        throw new InvalidBidException("Could not place bid");
    }

    @Override
    public List<BidHistoryDto> getBidHistory(UUID auctionId) {
        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId)
                .stream()
                .map(this::toBidHistoryItem)
                .toList();
    }

    @Override
    public List<BidHistoryDto> getBidHistory(UUID auctionId, int limit) {
        int safeLimit = limit <= 0 ? 50 : limit;
        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId)
                .stream()
                .limit(safeLimit)
                .map(this::toBidHistoryItem)
                .toList();
    }

    @Override
    public AuctionSummaryResponse getAuctionSummary(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        AuctionSummaryResponse response = new AuctionSummaryResponse();
        response.setAuctionId(auction.getId());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setMinNextBid(auction.getCurrentPrice() + minIncrement);
        response.setBidCount(bidRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId).size());
        response.setLeaderId(auction.getLeaderId());
        response.setLeaderName(resolveUserName(auction.getLeaderId()));
        response.setEndTime(auction.getEndTime());
        response.setTimeRemainingSeconds(resolveRemainingSeconds(auctionId, auction));
        response.setState(auction.getState() == null ? null : auction.getState().name());
        return response;
    }

    @Override
    public UUID getCurrentLeader(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        return auction.getLeaderId();
    }

    @Override
    public double getMinIncrement() {
        return minIncrement;
    }

    protected BidTransaction placeBidWithInMemoryLock(UUID auctionId, UUID bidderId, double amount) {
        ReentrantLock lock = lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
        lock.lock();
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

            validateAuctionForBid(auction, bidderId, amount);

            double minAllowed = auction.getCurrentPrice() + minIncrement;
            if (amount < minAllowed) {
                throw new InvalidBidException("Bid must be at least " + minAllowed);
            }

            UUID previousLeader = auction.getLeaderId();
            extendAuctionIfNeeded(auction);
            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            BidTransaction saved = bidRepository.save(new BidTransaction(auctionId, bidderId, amount, Instant.now()));
            applyAutoBidIfNeeded(auction, bidderId);

            if (eventPublisher != null) {
                try {
                    eventPublisher.publishBidPlaced(auctionId, bidderId, amount, previousLeader, Instant.now());
                } catch (Exception e) {
                    log.warn("Failed to publish fallback bid event for auction {}", auctionId, e);
                }
            }

            return saved;
        } finally {
            lock.unlock();
        }
    }

    private void validateAuctionForBid(Auction auction, UUID bidderId, double amount) {
        if (auction == null) {
            throw new ResourceNotFoundException("Auction is missing");
        }
        if (auction.getState() == AuctionState.FINISHED
                || auction.getState() == AuctionState.CANCELLED
                || auction.getState() == AuctionState.REJECTED
                || auction.getState() == AuctionState.DELETED) {
            throw new AuctionClosedException("Auction is closed");
        }

        Instant now = Instant.now();
        UUID sellerId = auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy();
        if (sellerId != null && sellerId.equals(bidderId)) {
            throw new InvalidBidException("Seller cannot bid on their own auction");
        }
        bidWalletService.ensureSufficientBalanceForBid(bidderId, amount);
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction has not started");
        }
        if (auction.getState() == AuctionState.SCHEDULED && auction.getStartTime() != null && !now.isBefore(auction.getStartTime())) {
            auction.setState(AuctionState.ACTIVE);
            auctionRepository.save(auction);
        }
        if (auction.getState() != AuctionState.ACTIVE) {
            throw new InvalidBidException("Auction is not active");
        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            bidWalletService.captureWinnerPayment(auction);
            auctionRepository.save(auction);
            if (eventPublisher != null) {
                try {
                    eventPublisher.publishAuctionClosed(auction.getId(), auction.getWinnerId(), auction.getCurrentPrice(), Instant.now());
                } catch (Exception e) {
                    log.warn("Failed to publish fallback auction closed event for auction {}", auction.getId(), e);
                }
            }
            throw new AuctionClosedException("Auction has ended");
        }
    }

    private void applyAutoBidIfNeeded(Auction auction, UUID triggeringBidderId) {
        List<AutoBid> autoBids = autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auction.getId());
        AutoBid best = null;
        double secondBestLimit = auction.getCurrentPrice();

        for (AutoBid autoBid : autoBids) {
            if (autoBid.getBidderId().equals(triggeringBidderId)) {
                continue;
            }
            double autoBidStep = resolveAutoBidStep(autoBid);
            if (autoBid.getMaxAmount() < auction.getCurrentPrice() + autoBidStep) {
                continue;
            }
            if (best == null) {
                best = autoBid;
            } else {
                secondBestLimit = Math.max(secondBestLimit, autoBid.getMaxAmount());
                break;
            }
        }

        if (best == null) {
            return;
        }

        double bestStep = resolveAutoBidStep(best);
        double autoAmount = Math.min(best.getMaxAmount(), secondBestLimit + bestStep);
        if (autoAmount <= auction.getCurrentPrice()) {
            return;
        }

        UUID previousLeader = auction.getLeaderId();
        auction.setCurrentPrice(autoAmount);
        auction.setLeaderId(best.getBidderId());
        auctionRepository.save(auction);
        bidRepository.save(new BidTransaction(auction.getId(), best.getBidderId(), autoAmount, Instant.now()));

        if (eventPublisher != null) {
            try {
                eventPublisher.publishAutoBidPlaced(auction.getId(), best.getBidderId(), autoAmount, previousLeader, Instant.now());
            } catch (Exception e) {
                log.warn("Failed to publish auto-bid fallback event for auction {}", auction.getId(), e);
            }
        }
    }

    private void extendAuctionIfNeeded(Auction auction) {
        Instant now = Instant.now();
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) {
            return;
        }

        long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        if (secondsLeft <= antiSnipingThresholdSeconds) {
            auction.setEndTime(auction.getEndTime().plusSeconds(antiSnipingExtendSeconds));
        }
    }

    private void backoffSleep(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private double resolveAutoBidStep(AutoBid autoBid) {
        if (autoBid == null) {
            return minIncrement;
        }

        return Math.max(minIncrement, autoBid.resolveBidStep(minIncrement));
    }

    private BidHistoryDto toBidHistoryItem(BidTransaction tx) {
        if (auctionHelper != null) {
            return auctionHelper.toBidHistoryItem(tx);
        }

        BidHistoryDto dto = new BidHistoryDto();
        dto.setBidId(tx.getId());
        dto.setAuctionId(tx.getAuctionId());
        dto.setBidderId(tx.getBidderId());
        dto.setBidderName(resolveUserName(tx.getBidderId()));
        dto.setAmount(tx.getAmount());
        dto.setCreatedAt(tx.getCreatedAt());
        boolean autoBid = autoBidRepository.existsByAuctionIdAndBidderIdAndActiveTrue(tx.getAuctionId(), tx.getBidderId());
        dto.setAutoBid(autoBid);
        dto.setSource(autoBid ? "AUTO_BID" : "MANUAL_BID");
        return dto;
    }

    private String resolveUserName(UUID userId) {
        if (auctionHelper != null) {
            return auctionHelper.lookupUserName(userId);
        }
        return userId == null ? null : userId.toString();
    }

    private long resolveRemainingSeconds(UUID auctionId, Auction auction) {
        if (auctionHelper != null) {
            return auctionHelper.computeRemainingSeconds(auctionId);
        }
        if (auction == null || auction.getEndTime() == null) {
            return 0L;
        }
        return Math.max(0L, java.time.Duration.between(Instant.now(), auction.getEndTime()).getSeconds());
    }
}
