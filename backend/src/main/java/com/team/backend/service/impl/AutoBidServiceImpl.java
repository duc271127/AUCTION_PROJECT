package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AutoBid;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.service.AutoBidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AutoBidServiceImpl implements AutoBidService {

    private static final Logger log = LoggerFactory.getLogger(AutoBidServiceImpl.class);

    private final AutoBidRepository autoBidRepository;
    private final AuctionRepository auctionRepository;

    public AutoBidServiceImpl(AutoBidRepository autoBidRepository,
                              AuctionRepository auctionRepository) {
        this.autoBidRepository = autoBidRepository;
        this.auctionRepository = auctionRepository;
    }

    @Override
    @Transactional
    public AutoBid setAutoBid(UUID auctionId, UUID bidderId, double maxAmount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId và bidderId là bắt buộc");
        }
        if (maxAmount <= 0.0) {
            throw new InvalidBidException("maxAmount phải lớn hơn 0");
        }

        // Kiểm tra auction tồn tại và chưa kết thúc
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction không tồn tại: " + auctionId));

        Instant now = Instant.now();
        if (auction.getState() != null &&
                (auction.getState().name().equalsIgnoreCase("FINISHED")
                        || auction.getState().name().equalsIgnoreCase("CANCELLED"))) {
            throw new InvalidBidException("Không thể đặt auto-bid: auction đã đóng");
        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            throw new InvalidBidException("Không thể đặt auto-bid: auction đã kết thúc theo thời gian");
        }

        // Xử lý theo repository trả về List<AutoBid>
        List<AutoBid> existing = autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId);
        AutoBid autoBid;
        if (existing != null && !existing.isEmpty()) {
            autoBid = existing.get(0);
            autoBid.setMaxAmount(maxAmount);
            autoBid.setActive(true);
            // giữ createdAt để tie-breaker không thay đổi
            log.info("Cập nhật AutoBid: auction={}, bidder={}, maxAmount={}", auctionId, bidderId, maxAmount);
        } else {
            autoBid = new AutoBid(auctionId, bidderId, maxAmount);
            log.info("Tạo AutoBid mới: auction={}, bidder={}, maxAmount={}", auctionId, bidderId, maxAmount);
        }

        return autoBidRepository.save(autoBid);
    }

    @Override
    @Transactional
    public void cancelAutoBid(UUID auctionId, UUID bidderId) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId và bidderId là bắt buộc");
        }

        List<AutoBid> existing = autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId);
        if (existing != null && !existing.isEmpty()) {
            for (AutoBid a : existing) {
                a.setActive(false);
                autoBidRepository.save(a);
            }
            log.info("Đã hủy AutoBid: auction={}, bidder={}", auctionId, bidderId);
        } else {
            log.info("Không tìm thấy AutoBid để hủy: auction={}, bidder={}", auctionId, bidderId);
        }
    }

    @Transactional(readOnly = true)
    public List<AutoBid> listAutoBidsForAuction(UUID auctionId) {
        return autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId);
    }
}
