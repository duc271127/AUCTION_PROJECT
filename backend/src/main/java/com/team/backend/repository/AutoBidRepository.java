package com.team.backend.repository;

import com.team.backend.entity.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AutoBidRepository extends JpaRepository<AutoBid, UUID> {

    List<AutoBid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    List<AutoBid> findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(UUID auctionId);

    List<AutoBid> findByBidderIdAndActiveTrueOrderByCreatedAtAsc(UUID bidderId);

    boolean existsByAuctionIdAndBidderIdAndActiveTrue(UUID auctionId, UUID bidderId);
}
