package com.auction.server.service;

import com.auction.server.dto.AuctionDetailResponse;
import com.auction.server.entity.Favorite;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.realtime.RealtimeEvent;
import com.auction.server.realtime.RealtimeEventFactory;
import com.auction.server.realtime.RealtimeNotifier;
import com.auction.server.repository.FavoriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final AuctionService auctionService;
    private final RealtimeNotifier realtimeNotifier;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           AuctionService auctionService,
                           RealtimeNotifier realtimeNotifier) {
        this.favoriteRepository = favoriteRepository;
        this.auctionService = auctionService;
        this.realtimeNotifier = realtimeNotifier;
    }

    public void add(UUID userId, UUID auctionId) {
        if (userId == null || auctionId == null) {
            throw new BusinessRuleException("userId and auctionId are required");
        }

        auctionService.getAuction(auctionId);

        if (favoriteRepository.existsByUserIdAndAuctionId(userId, auctionId)) {
            return;
        }

        favoriteRepository.save(new Favorite(userId, auctionId));
        publishFavoriteChanged(auctionId, "Favorite count updated.");
    }

    public void remove(UUID userId, UUID auctionId) {
        if (userId == null || auctionId == null) {
            throw new BusinessRuleException("userId and auctionId are required");
        }
        favoriteRepository.deleteByUserIdAndAuctionId(userId, auctionId);
        publishFavoriteChanged(auctionId, "Favorite count updated.");
    }

    public List<AuctionDetailResponse> list(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        List<AuctionDetailResponse> result = new ArrayList<>();
        for (Favorite favorite : favorites) {
            try {
                result.add(auctionService.getDetail(favorite.getAuctionId()));
            } catch (Exception ex) {
                log.warn("Skipping broken favorite {}", favorite.getAuctionId(), ex);
            }
        }
        return result;
    }

    private void publishFavoriteChanged(UUID auctionId, String message) {
        try {
            AuctionDetailResponse detail = auctionService.getDetail(auctionId);
            long remainingSeconds = 0L;
            if (detail != null && detail.endTime != null) {
                remainingSeconds = Math.max(0L, java.time.Duration.between(java.time.Instant.now(), detail.endTime).getSeconds());
            }

            RealtimeEvent event = RealtimeEventFactory.favoriteChanged(
                    auctionId,
                    detail == null ? null : detail.leaderId,
                    detail == null ? null : detail.leaderName,
                    detail == null ? 0.0 : detail.currentPrice,
                    detail == null ? 0L : detail.favoriteCount,
                    detail == null ? null : detail.state,
                    remainingSeconds,
                    detail == null ? null : detail.endTime,
                    message
            );
            realtimeNotifier.broadcastToAuction(auctionId, event);
        } catch (Exception ex) {
            log.warn("Failed to publish favorite event for auction {}", auctionId, ex);
        }
    }
}

