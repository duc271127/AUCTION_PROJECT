package com.team.backend.service.impl;
import com.team.backend.dto.BidHistoryDto;

import com.team.backend.dto.BidHistoryDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.stream.Collectors;

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

    // Nếu bạn muốn giữ eventPublisher là nullable, có thể lưu dưới dạng null hoặc Optional
    private final EventPublisher eventPublisher;

    public BidServiceImpl(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          AutoBidRepository autoBidRepository,
                          BidTransactionalService bidTransactionalService,
                          @Value("${auction.bid.min-increment:1.0}") double minIncrement,
                          @Value("${auction.anti-sniping.threshold-seconds:30}") long antiSnipingThresholdSeconds,
                          @Value("${auction.anti-sniping.extend-seconds:60}") long antiSnipingExtendSeconds,
                          @Value("${auction.bid.max-retries:3}") int maxRetries,
                          Optional<EventPublisher> eventPublisherOptional) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
        this.bidTransactionalService = bidTransactionalService;
        this.minIncrement = minIncrement;
        this.antiSnipingThresholdSeconds = antiSnipingThresholdSeconds;
        this.antiSnipingExtendSeconds = antiSnipingExtendSeconds;
        this.maxRetries = Math.max(1, maxRetries);
        this.eventPublisher = eventPublisherOptional.orElse(null);
    }

    private ReentrantLock getLock(UUID auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    /**
     * Public entry point để đặt giá.
     * Thực hiện retry khi gặp xung đột optimistic/pessimistic.
     * Nếu quá nhiều lỗi lock, fallback sang in-memory lock (chỉ an toàn cho single-instance).
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
                // Ủy quyền cho transactional service (đảm bảo @Transactional hoạt động)
                BidTransaction tx = bidTransactionalService.placeBidTransactionalAttempt(
                        auctionId, bidderId, amount, minIncrement, antiSnipingThresholdSeconds, antiSnipingExtendSeconds, eventPublisher);
                log.info("Đặt giá thành công (transactional): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
                return tx;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Xung đột optimistic attempt {} cho auction {}: {}", attempt, auctionId, ex.getMessage());
                if (attempt >= maxRetries) {
                    throw new InvalidBidException("Xung đột đồng thời, vui lòng thử lại sau");
                }
                backoffSleep(attempt);
            } catch (PessimisticLockingFailureException ex) {
                log.warn("Pessimistic lock failure attempt {} cho auction {}: {}", attempt, auctionId, ex.getMessage());
                if (attempt >= maxRetries) {
                    log.info("Chuyển sang khóa trong bộ nhớ cho auction {}", auctionId);
                    return placeBidWithInMemoryLock(auctionId, bidderId, amount);
                }
                backoffSleep(attempt);
            } catch (ResourceNotFoundException | InvalidBidException | AuctionClosedException ex) {
                // lỗi nghiệp vụ: ném ngay
                throw ex;
            } catch (RuntimeException ex) {
                // lỗi bất ngờ: fallback sang in-memory lock
                log.warn("Lỗi bất ngờ ở attempt {} cho auction {}: {}, chuyển sang in-memory lock", attempt, auctionId, ex.getMessage());
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
     * Fallback: sử dụng ReentrantLock trong bộ nhớ cho mỗi auction.
     * Chỉ an toàn cho môi trường single-instance hoặc testing.
     */
    protected BidTransaction placeBidWithInMemoryLock(UUID auctionId, UUID bidderId, double amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy auction: " + auctionId));

            // validate trạng thái và thời gian
            validateAuctionForBid(auction);

            double minAllowed = auction.getCurrentPrice() + minIncrement;
            if (amount < minAllowed) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
            }

            // anti-sniping: kéo dài nếu cần
            extendAuctionIfNeeded(auction);

            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
            BidTransaction saved = bidRepository.save(tx);

            // áp dụng auto-bid đồng bộ (cùng instance)
            applyAutoBidIfNeeded(auction, bidderId);

            // publish event best-effort (không có afterCommit ở fallback)
            if (eventPublisher != null) {
                try {
                    eventPublisher.publishBidPlaced(auctionId, bidderId, amount, null, Instant.now());
                } catch (Exception e) {
                    log.warn("EventPublisher thất bại trong fallback path: {}", e.getMessage());
                }
            }

            log.debug("Đã đặt giá (in-memory lock): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Validate auction trước khi chấp nhận bid (dùng cho fallback path).
     */
    private void validateAuctionForBid(Auction auction) {
        if (auction == null) {
            throw new ResourceNotFoundException("Auction là null");
        }
        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELLED) {
            throw new AuctionClosedException("Auction đã đóng");
        }
        Instant now = Instant.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction chưa bắt đầu");
        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            // đánh dấu finished và persist
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auctionRepository.save(auction);
            throw new AuctionClosedException("Auction đã kết thúc");
        }
    }

    /**
     * Auto-bid logic cho fallback path (đồng bộ).
     * Nếu auto-bid đã được xử lý trong transactional path thì đây sẽ là no-op trong luồng bình thường.
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
                log.warn("EventPublisher thất bại khi publish auto-bid: {}", e.getMessage());
            }
        }
    }

    /**
     * Anti-sniping: kéo dài endTime nếu thời gian còn lại <= threshold.
     * Dùng cho fallback path; transactional path xử lý bên trong transaction.
     */
    private void extendAuctionIfNeeded(Auction auction) {
        Instant now = Instant.now();
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) return;
        long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
        if (secondsLeft <= antiSnipingThresholdSeconds) {
            auction.setEndTime(auction.getEndTime().plusSeconds(antiSnipingExtendSeconds));
            log.debug("Anti-sniping: đã kéo dài auction {} thêm {} giây", auction.getId(), antiSnipingExtendSeconds);
        }
    }

    /**
     * Lấy lịch sử bid (trả DTO BidHistoryDto, không trả entity thô).
     * Trả về danh sách theo thứ tự mới nhất trước (desc).
     */
    @Override
    public List<BidHistoryDto> getBidHistory(UUID auctionId) {

        List<BidTransaction> transactions =
                bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId);

        return transactions.stream()
                .map(tx -> new BidHistoryDto(
                        tx.getBidderId(),
                        tx.getAmount(),
                        tx.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<BidHistoryDto> getBidHistory(UUID auctionId, int limit) {
        if (limit <= 0) limit = 50;
        List<BidTransaction> transactions = bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId);
        return transactions.stream()
                .limit(limit)
                .map(tx -> new BidHistoryDto(tx.getBidderId(), tx.getAmount(), tx.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAuctionSummary(UUID auctionId) {
        Auction a = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy auction: " + auctionId));
        Map<String, Object> m = new HashMap<>();
        m.put("auctionId", a.getId());
        m.put("currentPrice", a.getCurrentPrice());
        m.put("minNext", a.getCurrentPrice() + minIncrement);
        m.put("leaderId", a.getLeaderId());
        m.put("endTime", a.getEndTime());
        return m;
    }

    @Override
    public UUID getCurrentLeader(UUID auctionId) {
        Auction a = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy auction: " + auctionId));
        return a.getLeaderId();
    }

    @Override
    public double getMinIncrement() {
        return minIncrement;
    }

}
