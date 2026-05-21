package com.team.backend.service;

import com.team.backend.entity.BidTransaction;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BidService {
    BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount);
<<<<<<< Updated upstream
    List<BidTransaction> getBidHistory(UUID auctionId);
=======
    List<BidHistoryDto> getBidHistory(UUID auctionId);
    Map<String, Object> getAuctionSummary(UUID auctionId);
    UUID getCurrentLeader(UUID auctionId);
    double getMinIncrement();

>>>>>>> Stashed changes
}
