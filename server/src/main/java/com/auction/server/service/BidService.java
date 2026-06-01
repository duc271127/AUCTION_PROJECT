package com.auction.server.service;

import com.auction.server.dto.AuctionSummaryResponse;
import com.auction.server.dto.BidHistoryDto;
import com.auction.server.entity.BidTransaction;

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

