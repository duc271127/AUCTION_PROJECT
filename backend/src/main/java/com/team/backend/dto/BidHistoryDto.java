package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class BidHistoryDto {

    private UUID bidderId;
    private String bidderName;
    private double amount;
    private Instant createdAt;

    public BidHistoryDto() {
    }

    public BidHistoryDto(UUID bidderId, double amount, Instant createdAt) {
        this.bidderId = bidderId;
        this.amount = amount;
        this.createdAt = createdAt;
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
}
