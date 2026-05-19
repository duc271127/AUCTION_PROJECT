package com.team.backend.service;

import java.time.Instant;
import java.util.UUID;

public interface EventPublisher {
    void publishBidPlaced(UUID auctionId, UUID bidderId, double amount, UUID previousLeader, Instant timestamp) throws Exception;
    void publishAuctionExtended(UUID auctionId, double newPrice, Instant newEndTime) throws Exception;
    void publishAutoBidPlaced(UUID auctionId, UUID bidderId, double amount, Instant timestamp) throws Exception;
}
