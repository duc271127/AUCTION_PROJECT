package com.auction.server.repository;

import com.auction.server.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByUserId(UUID userId);
    long countByAuctionId(UUID auctionId);
    @Query("select f.auctionId as auctionId, count(f) as count from Favorite f where f.auctionId in :auctionIds group by f.auctionId")
    List<AuctionCountProjection> countByAuctionIds(@Param("auctionIds") Collection<UUID> auctionIds);
    boolean existsByUserIdAndAuctionId(UUID userId, UUID auctionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("delete from Favorite f where f.userId = :userId and f.auctionId = :auctionId")
    int deleteByUserIdAndAuctionId(@Param("userId") UUID userId, @Param("auctionId") UUID auctionId);

    void deleteByAuctionId(UUID auctionId);
}

