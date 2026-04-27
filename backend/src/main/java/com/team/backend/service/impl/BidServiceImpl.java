package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.BidTransaction;
import com.team.backend.exception.AuctionClosedException;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.service.BidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BidServiceImpl - phiên bản đã sửa lỗi
 */
@Service
public class BidServiceImpl implements BidService {

    private static final Logger log = LoggerFactory.getLogger(BidServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    private final ConcurrentHashMap<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final double minIncrement;
    private final int maxRetries;

    public BidServiceImpl(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          @Value("${auction.bid.min-increment:1.0}") double minIncrement,
                          @Value("${auction.bid.lock-retries:3}") int maxRetries) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.minIncrement = minIncrement;
        this.maxRetries = Math.max(1, maxRetries);
    }

    private ReentrantLock getLock(UUID auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    @Override
    @Transactional
    public BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId và bidderId là bắt buộc");
        }
        if (amount <= 0) {
            throw new InvalidBidException("Số tiền đặt phải lớn hơn 0");
        }

        // retry loop với số lần cố định, không dùng while(true)
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Thử dùng DB-level pessimistic lock nếu repository hỗ trợ
                Auction auction = tryLoadAuctionForUpdate(auctionId);

                // Validate và cập nhật
                validateAuctionForBid(auction);

                double minAllowed = auction.getCurrentPrice() + minIncrement;
                if (amount < minAllowed) {
                    throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
                }

                auction.setCurrentPrice(amount);
                auction.setLeaderId(bidderId);
                auctionRepository.save(auction);

                BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
                BidTransaction saved = bidRepository.save(tx);

                log.debug("Đặt giá thành công (DB lock): auction={}, bidder={}, amount={}, attempt={}",
                        auctionId, bidderId, amount, attempt);
                return saved;

            } catch (ObjectOptimisticLockingFailureException | PessimisticLockingFailureException lockEx) {
                log.warn("Xung đột khóa khi đặt giá (attempt {}): {}", attempt, lockEx.getMessage());
                if (attempt >= maxRetries) {
                    throw new InvalidBidException("Không thể đặt giá do xung đột đồng thời, vui lòng thử lại");
                }
                // backoff ngắn trước khi retry
                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new InvalidBidException("Đặt giá bị gián đoạn");
                }
                // tiếp tục vòng retry
            } catch (ResourceNotFoundException | InvalidBidException | AuctionClosedException ex) {
                // Các lỗi nghiệp vụ ném ngay
                throw ex;
            } catch (RuntimeException ex) {
                // Nếu DB lock không khả dụng hoặc lỗi runtime khác -> fallback 1 lần vào in-memory lock
                log.debug("Lỗi khi dùng DB lock, fallback sang in-memory lock: {}", ex.getMessage());
                return placeBidWithInMemoryLock(auctionId, bidderId, amount);
            }
        }

        // Không nên tới đây, nhưng để an toàn:
        throw new InvalidBidException("Không thể đặt giá, vui lòng thử lại sau");
    }

    /**
     * Thử load auction bằng phương thức findByIdForUpdate của repository.
     * Nếu repository không cung cấp method này, phương thức sẽ ném RuntimeException để caller fallback.
     */
    private Auction tryLoadAuctionForUpdate(UUID auctionId) {
        try {
            // AuctionRepository nên định nghĩa Optional<Auction> findByIdForUpdate(UUID id);
            return auctionRepository.findByIdForUpdate(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        } catch (UnsupportedOperationException | PessimisticLockingFailureException ex) {
            // ném để caller biết cần fallback
            throw new RuntimeException("DB lock không khả dụng: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fallback: dùng in-memory ReentrantLock per-auction.
     * Lưu ý: không an toàn trong multi-instance; chỉ dùng khi DB lock không khả dụng.
     */
    @Transactional
    protected BidTransaction placeBidWithInMemoryLock(UUID auctionId, UUID bidderId, double amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

            validateAuctionForBid(auction);

            double minAllowed = auction.getCurrentPrice() + minIncrement;
            if (amount < minAllowed) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
            }

            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
            BidTransaction saved = bidRepository.save(tx);

            log.debug("Đặt giá thành công (in-memory lock): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    private void validateAuctionForBid(Auction auction) {
        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELED) {
            throw new AuctionClosedException("Auction đã đóng");
        }
        Instant now = Instant.now();
        if (now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction chưa bắt đầu");
        }
        if (now.isAfter(auction.getEndTime())) {
            // đánh dấu finished phòng ngừa và lưu
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auctionRepository.save(auction);
            throw new AuctionClosedException("Auction đã kết thúc");
        }
    }

    @Override
    public List<BidTransaction> getBidHistory(UUID auctionId) {
        // Điều chỉnh tên method repository nếu cần
        return bidRepository.findByAuctionIdOrderByTimestampAsc(auctionId);
    }
}
