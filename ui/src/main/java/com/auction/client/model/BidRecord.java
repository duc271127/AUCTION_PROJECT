package com.auction.client.model;

public class BidRecord {
    private final String bidderName;
    private final String bidAmount;
    private final String bidTime;

    public BidRecord(String bidderName, String bidAmount, String bidTime) {
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getBidderName() {
        return bidderName;
    }

    public String getBidAmount() {
        return bidAmount;
    }

    public String getBidTime() {
        return bidTime;
    }

    @Override
    public String toString() {
        return bidderName + " • " + bidAmount + " • " + bidTime;
    }
}