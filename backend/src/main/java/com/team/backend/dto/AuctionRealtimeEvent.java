package com.team.backend.dto;

import java.util.UUID;

public class AuctionRealtimeEvent {
    public UUID auctionId;
    public double currentPrice;
    public String leaderName;
    public long remainingSeconds;
    public String eventId;
    public BidHistoryDto latestBid;
}
