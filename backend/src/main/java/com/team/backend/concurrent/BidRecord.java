package com.team.backend.concurrent;

import java.time.LocalDateTime;

public class BidRecord {

    private String bidderName;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidRecord(String bidderName, double bidAmount, LocalDateTime bidTime) {
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    @Override
    public String toString() {
        return "BidRecord{" +
                "bidderName='" + bidderName + '\'' +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                '}';
    }
}