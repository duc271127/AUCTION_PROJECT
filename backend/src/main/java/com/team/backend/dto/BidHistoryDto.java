package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class BidHistoryDto {

    private UUID bidId;
    private UUID auctionId;
    private UUID bidderId;
    private String bidderName;
    private double amount;
    private Instant createdAt;
    private String source;
    private boolean autoBid;

    public BidHistoryDto() {
    }

    public BidHistoryDto(UUID bidderId, double amount, Instant createdAt) {
        this.bidderId = bidderId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public UUID getBidId() {
        return bidId;
    }

    public void setBidId(UUID bidId) {
        this.bidId = bidId;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isAutoBid() {
        return autoBid;
    }

    public void setAutoBid(boolean autoBid) {
        this.autoBid = autoBid;
    }
}
