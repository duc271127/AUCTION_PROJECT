package com.auction.client.dto.request;

public class AutoBidRequest {
    private double maxAmount;

    public AutoBidRequest() {
    }

    public AutoBidRequest(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }
}
