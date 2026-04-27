package com.team.backend.repository;

import com.team.backend.entity.BidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BidRepository extends JpaRepository<BidTransaction, UUID> {
    List<BidTransaction> findByAuctionIdOrderByTimestampAsc(UUID auctionId);
    List<BidTransaction> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);

}
