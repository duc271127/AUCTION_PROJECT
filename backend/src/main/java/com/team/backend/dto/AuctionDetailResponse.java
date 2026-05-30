package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class AuctionDetailResponse {
    public UUID id;
    public UUID itemId;
    public String title;
    public String description;
    public String imageUrl;
    public String category;
    public UUID sellerId;
    public String sellerName;
    public UUID leaderId;
    public int bidCount;
    public double currentPrice;
    public double reservePrice;
    public boolean reserveMet;
    public long viewCount;
    public long favoriteCount;
    public double trendingScore;
    public double minNextBid;
    public String leaderName;
    public String state;
    public Instant startTime;
    public Instant endTime;
}
