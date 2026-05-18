package com.auction.client.dto.request;

import java.util.UUID;

public class AutoBidRequest {
    private UUID bidderId;
    private double maxAmount;

    public AutoBidRequest() {
    }

    public AutoBidRequest(UUID bidderId, double maxAmount) {
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }
}