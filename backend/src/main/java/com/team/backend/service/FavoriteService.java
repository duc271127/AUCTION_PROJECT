package com.team.backend.service;

import com.team.backend.dto.AuctionDetailResponse;
import com.team.backend.entity.Favorite;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.FavoriteRepository;
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

    public FavoriteService(FavoriteRepository favoriteRepository,
                           AuctionService auctionService) {
        this.favoriteRepository = favoriteRepository;
        this.auctionService = auctionService;
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
    }

    public void remove(UUID userId, UUID auctionId) {
        if (userId == null || auctionId == null) {
            throw new BusinessRuleException("userId and auctionId are required");
        }
        favoriteRepository.deleteByUserIdAndAuctionId(userId, auctionId);
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
}
