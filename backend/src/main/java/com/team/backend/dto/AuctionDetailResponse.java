package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class AuctionDetailResponse {
    public UUID id;
    public String title;
    public String description;
    public String imageUrl;
    public String category;
    public UUID sellerId;
    public int bidCount;
    public double currentPrice;
    public double minNextBid;
    public String leaderName;
    public String state;
    public Instant startTime;
    public Instant endTime;
}
