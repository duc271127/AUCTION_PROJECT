package com.team.backend.repository;

import com.team.backend.entity.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutoBidRepository extends JpaRepository<AutoBid, UUID> {

    Optional<AutoBid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    List<AutoBid> findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(UUID auctionId);
}