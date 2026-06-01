package com.auction.client.dto.request;

public class AutoBidRequest {
    private double maxAmount;
    private double bidStep;

    public AutoBidRequest() {
    }

    public AutoBidRequest(double maxAmount, double bidStep) {
        this.maxAmount = maxAmount;
        this.bidStep = bidStep;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getBidStep() {
        return bidStep;
    }

    public void setBidStep(double bidStep) {
        this.bidStep = bidStep;
    }
}
