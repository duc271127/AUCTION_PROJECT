package com.team.backend.service.impl;

import com.team.backend.entity.AutoBid;
import com.team.backend.entity.Auction;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.service.AutoBidService;
import com.team.backend.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AutoBidServiceImpl implements AutoBidService {

    private static final Logger log = LoggerFactory.getLogger(AutoBidServiceImpl.class);

    private final AutoBidRepository autoBidRepository;
    private final AuctionRepository auctionRepository;
    private final EventPublisher eventPublisher; // có thể null nếu không có bean

    public AutoBidServiceImpl(AutoBidRepository autoBidRepository,
                              AuctionRepository auctionRepository,
                              ObjectProvider<EventPublisher> eventPublisherProvider) {
        this.autoBidRepository = autoBidRepository;
        this.auctionRepository = auctionRepository;
        this.eventPublisher = eventPublisherProvider.getIfAvailable();
    }

    /**
     * Thiết lập hoặc cập nhật auto-bid cho một auction và bidder.
     * Nếu đã tồn tại auto-bid cho cặp (auctionId, bidderId) thì cập nhật maxAmount và active=true.
     * Trả về entity AutoBid đã lưu.
     */
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

        List<AutoBid> existing = autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId);
        AutoBid autoBid;
        boolean created = false;
        if (existing != null && !existing.isEmpty()) {
            autoBid = existing.get(0);
            autoBid.setMaxAmount(maxAmount);
            autoBid.setActive(true);
            autoBid.setUpdatedAt(Instant.now());
            log.info("Cập nhật AutoBid: auction={}, bidder={}, maxAmount={}", auctionId, bidderId, maxAmount);
        } else {
            autoBid = new AutoBid(auctionId, bidderId, maxAmount);
            autoBid.setCreatedAt(Instant.now());
            autoBid.setUpdatedAt(Instant.now());
            created = true;
            log.info("Tạo AutoBid mới: auction={}, bidder={}, maxAmount={}", auctionId, bidderId, maxAmount);
        }

        AutoBid saved = autoBidRepository.save(autoBid);

        // Publish sự kiện (best-effort)
        if (eventPublisher != null) {
            try {
                eventPublisher.publishAutoBidPlaced(auctionId, bidderId, saved.getMaxAmount(), Instant.now());
            } catch (Exception e) {
                log.warn("EventPublisher thất bại khi publish sự kiện auto-bid cho auction {}: {}", auctionId, e.getMessage());
            }
        }

        return saved;
    }

    /**
     * Hủy auto-bid cho cặp (auctionId, bidderId).
     * Nếu không tìm thấy, không ném lỗi mà log và trả về.
     */
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
                a.setUpdatedAt(Instant.now());
                autoBidRepository.save(a);
            }
            log.info("Đã hủy AutoBid: auction={}, bidder={}", auctionId, bidderId);

            if (eventPublisher != null) {
                try {
                    // Thông báo hủy auto-bid (nếu EventPublisher hỗ trợ event tương ứng)
                    eventPublisher.publishAutoBidPlaced(auctionId, bidderId, 0.0, Instant.now());
                } catch (Exception e) {
                    log.warn("EventPublisher thất bại khi publish hủy auto-bid cho auction {}: {}", auctionId, e.getMessage());
                }
            }
        } else {
            log.info("Không tìm thấy AutoBid để hủy: auction={}, bidder={}", auctionId, bidderId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutoBid> listAutoBidsForAuction(UUID auctionId) {
        if (auctionId == null) throw new IllegalArgumentException("auctionId là bắt buộc");
        return autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutoBid> listAutoBidsByUser(UUID bidderId) {
        if (bidderId == null) throw new IllegalArgumentException("bidderId là bắt buộc");
        return autoBidRepository.findByBidderIdAndActiveTrueOrderByCreatedAtAsc(bidderId);
    }
}
