package com.auction.client.dto.request;
import java.util.UUID;


public class BidRequest {
    private UUID bidderId;
    private double amount;

    public BidRequest(UUID bidderId, double amount) {
        this.bidderId = bidderId;
        this.amount = amount;
    }
    public UUID getBidderId() {
        return bidderId;
    }
    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
