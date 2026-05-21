package com.team.backend.service;

import com.team.backend.dto.AuctionDetailResponse;
import com.team.backend.entity.Auction;
import com.team.backend.entity.Favorite;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.FavoriteRepository;
import com.team.backend.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           AuctionRepository auctionRepository,
                           AuctionService auctionService) {
        this.favoriteRepository = favoriteRepository;
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
    }

    /**
     * Thêm vào danh sách yêu thích. Nếu đã tồn tại thì không duplicate.
     */
    public void add(UUID userId, UUID auctionId) {
        if (userId == null || auctionId == null) {
            throw new IllegalArgumentException("userId và auctionId là bắt buộc");
        }
        boolean exists = favoriteRepository.existsByUserIdAndAuctionId(userId, auctionId);
        if (exists) {
            log.debug("Favorite đã tồn tại: user={}, auction={}", userId, auctionId);
            return;
        }
        Favorite f = new Favorite(userId, auctionId);
        favoriteRepository.save(f);
        log.info("Đã thêm vào yêu thích: user={}, auction={}", userId, auctionId);
    }

    /**
     * Xóa khỏi danh sách yêu thích.
     */
    public void remove(UUID userId, UUID auctionId) {
        if (userId == null || auctionId == null) {
            throw new IllegalArgumentException("userId và auctionId là bắt buộc");
        }
        favoriteRepository.deleteByUserIdAndAuctionId(userId, auctionId);
        log.info("Đã xóa khỏi yêu thích: user={}, auction={}", userId, auctionId);
    }

    /**
     * Trả về danh sách AuctionDetailResponse cho user.
     * Nếu auction không tồn tại (dữ liệu lỗi), sẽ bỏ qua.
     */
    public List<AuctionDetailResponse> list(UUID userId) {
        if (userId == null) return Collections.emptyList();
        List<Favorite> favs = favoriteRepository.findByUserId(userId);
        List<AuctionDetailResponse> out = new ArrayList<>();
        for (Favorite f : favs) {
            try {
                AuctionDetailResponse detail = auctionService.getDetail(f.getAuctionId());
                out.add(detail);
            } catch (Exception ex) {
                // nếu auction bị xóa hoặc lỗi, bỏ qua nhưng log
                log.warn("Không lấy được chi tiết auction cho favorite {}: {}", f.getAuctionId(), ex.getMessage());
            }
        }
        return out;
    }
}
