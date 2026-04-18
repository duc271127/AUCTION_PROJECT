package com.team.backend.repository;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findByState(AuctionState state);

    // auctions that should start now or earlier but still OPEN
    @Query("select a from Auction a where a.state = :state and a.startTime <= :now")
    List<Auction> findByStateAndStartTimeBefore(AuctionState state, Instant now);

    // auctions that should finish now or earlier but still RUNNING
    @Query("select a from Auction a where a.state = :state and a.endTime <= :now")
    List<Auction> findByStateAndEndTimeBefore(AuctionState state, Instant now);

    // optional: find active auctions overlapping now
    @Query("select a from Auction a where a.startTime <= :now and a.endTime > :now")
    List<Auction> findActiveAuctions(Instant now);
}
