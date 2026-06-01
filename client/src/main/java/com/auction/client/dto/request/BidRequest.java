package com.auction.client.dto.request;
public class BidRequest {
    private double amount;

    public BidRequest(double amount) {
        this.amount = amount;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
