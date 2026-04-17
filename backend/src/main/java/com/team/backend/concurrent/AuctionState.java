package com.team.backend.concurrent;

public class AuctionState {

    private Long auctionId;
    private double currentPrice;
    private String currentLeader;
    private String status;

    public AuctionState(Long auctionId, double currentPrice, String currentLeader, String status) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.currentLeader = currentLeader;
        this.status = status;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public String getStatus() {
        return status;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}