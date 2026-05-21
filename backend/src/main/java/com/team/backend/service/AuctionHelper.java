package com.team.backend.service;

import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.BidTransaction;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AuctionHelper - helper nhỏ để:
 * - lookup user display name (stub / tích hợp user service)
 * - compute remaining seconds cho auction
 * - chuyển BidTransaction -> BidHistoryItem
 *
 * Tùy chỉnh lookupUserName để gọi user service thực tế trong hệ thống của bạn.
 */
@Component
public class AuctionHelper {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionHelper(AuctionRepository auctionRepository, BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    /**
     * Tra cứu tên hiển thị của user.
     * Hiện tại trả fallback dạng "Người dùng-{shortId}" nếu không có service user.
     * Thay bằng call tới user service khi tích hợp thực tế.
     */
    public String lookupUserName(UUID userId) {
        if (userId == null) return null;
        // TODO: tích hợp user service ở đây
        String shortId = userId.toString().replace("-", "");
        if (shortId.length() > 8) shortId = shortId.substring(0, 8);
        return "Người dùng-" + shortId;
    }

    /**
     * Tính số giây còn lại cho auction (nếu endTime null => 0).
     */
    public long computeRemainingSeconds(UUID auctionId) {
        if (auctionId == null) return 0L;
        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) return 0L;
        Auction a = opt.get();
        Instant now = Instant.now();
        if (a.getEndTime() == null) return 0L;
        long seconds = Duration.between(now, a.getEndTime()).getSeconds();
        return Math.max(0L, seconds);
    }

    /**
     * Lấy danh sách lịch sử bid (dùng khi cần chuyển sang DTO).
     */
    public List<BidHistoryDto> toBidHistoryItems(UUID auctionId) {
        List<BidTransaction> txs = bidRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId);
        return txs.stream().map(tx -> toBidHistoryItem(tx.getBidderId(), tx.getAmount(), tx.getCreatedAt())).toList();
    }

    /**
     * Tạo 1 BidHistoryItem từ dữ liệu cơ bản.
     */
    public BidHistoryDto toBidHistoryItem(UUID bidderId, double amount, Instant createdAt) {
        BidHistoryDto h = new BidHistoryDto();
        h.setBidderId(bidderId);
        h.setBidderName(lookupUserName(bidderId));
        h.setAmount(amount);
        h.setCreatedAt(createdAt);
        return h;
    }
}
