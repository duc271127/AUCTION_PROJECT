package com.team.backend.repository;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findByState(AuctionState state);
}
