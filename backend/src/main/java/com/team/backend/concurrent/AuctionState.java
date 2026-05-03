package com.team.backend.concurrent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionState {

    private Long auctionId;
    private double currentPrice;
    private String currentLeader;
    private String status;

    private String winner;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private final List<BidRecord> bidHistory = new ArrayList<>();

    public AuctionState(Long auctionId, double currentPrice, String currentLeader, String status) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.currentLeader = currentLeader;
        this.status = status;
    }

    public AuctionState(Long auctionId,
                        double currentPrice,
                        String currentLeader,
                        String status,
                        LocalDateTime startTime,
                        LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.currentLeader = currentLeader;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public String getWinner() {
        return winner;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<BidRecord> getBidHistory() {
        return bidHistory;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
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

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void addBidRecord(BidRecord bidRecord) {
        this.bidHistory.add(bidRecord);
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(this.status);
    }

    public boolean isClosed() {
        return "CLOSED".equalsIgnoreCase(this.status);
    }
}