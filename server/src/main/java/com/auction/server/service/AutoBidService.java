package com.auction.server.service;

import com.auction.server.entity.AutoBid;

import java.util.List;
import java.util.UUID;

public interface AutoBidService {
    AutoBid setAutoBid(UUID auctionId, UUID bidderId, double maxAmount, double bidStep);
    void cancelAutoBid(UUID auctionId, UUID bidderId);
    List<AutoBid> listAutoBidsForAuction(UUID auctionId);
    List<AutoBid> listAutoBidsByUser(UUID bidderId);
}

