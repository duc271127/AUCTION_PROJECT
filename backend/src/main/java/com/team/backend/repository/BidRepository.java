package com.team.backend.repository;

import com.team.backend.entity.BidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BidRepository extends JpaRepository<BidTransaction, UUID> {
    List<BidTransaction> findByAuctionIdOrderByCreatedAtAsc(UUID auctionId);
    List<BidTransaction> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);
    List<BidTransaction> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);
    long countByAuctionId(UUID auctionId);
    @Query("select b.auctionId as auctionId, count(b) as count from BidTransaction b where b.auctionId in :auctionIds group by b.auctionId")
    List<AuctionCountProjection> countByAuctionIds(@Param("auctionIds") Collection<UUID> auctionIds);
    void deleteByAuctionId(UUID auctionId);
}
