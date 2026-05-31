package com.team.backend.service;

import com.team.backend.dto.AuctionSummaryResponse;
import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.BidTransaction;

import java.util.List;
import java.util.UUID;

public interface BidService {

    BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount);
    List<BidHistoryDto> getBidHistory(UUID auctionId);
    List<BidHistoryDto> getBidHistory(UUID auctionId, int limit);
    AuctionSummaryResponse getAuctionSummary(UUID auctionId);
    UUID getCurrentLeader(UUID auctionId);
    double getMinIncrement();
}
