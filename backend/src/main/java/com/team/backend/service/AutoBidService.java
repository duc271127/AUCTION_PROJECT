package com.team.backend.service;

import com.team.backend.entity.AutoBid;

import java.util.UUID;

public interface AutoBidService {
    AutoBid setAutoBid(UUID auctionId, UUID bidderId, double maxAmount);
}
public void cancelAutoBid(UUID auctionId, UUID bidderId) {

}
