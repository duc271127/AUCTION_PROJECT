package com.team.backend.repository;

import com.team.backend.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByUserId(UUID userId);
    long countByAuctionId(UUID auctionId);
    boolean existsByUserIdAndAuctionId(UUID userId, UUID auctionId);
    void deleteByUserIdAndAuctionId(UUID userId, UUID auctionId);
    void deleteByAuctionId(UUID auctionId);
}
