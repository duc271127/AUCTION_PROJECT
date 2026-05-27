package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class AuctionDto {
    public UUID id;
    public UUID itemId;
    public String itemName;
    public String title;
    public String description;
    public String imageUrl;
    public String category;
    public UUID sellerId;
    public String sellerName;
    public double currentPrice;
    public long viewCount;
    public long favoriteCount;
    public double trendingScore;
    public int bidCount;
    public double minNextBid;
    public UUID leaderId;
    public String leaderName;
    public UUID winnerId;
    public String winnerName;
    public Instant createdAt;
    public Instant startTime;
    public Instant endTime;
    public String state;
}
