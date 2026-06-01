package com.auction.server.dto;

import java.time.Instant;
import java.util.UUID;

public class BidPlacementResponse {
    private UUID auctionId;
    private UUID bidId;
    private UUID bidderId;
    private String bidderDisplay;
    private UUID leaderId;
    private String leaderName;
    private double currentPrice;
    private double minNextBid;
    private String state;
    private Instant endTime;

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getBidId() {
        return bidId;
    }

    public void setBidId(UUID bidId) {
        this.bidId = bidId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderDisplay() {
        return bidderDisplay;
    }

    public void setBidderDisplay(String bidderDisplay) {
        this.bidderDisplay = bidderDisplay;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getMinNextBid() {
        return minNextBid;
    }

    public void setMinNextBid(double minNextBid) {
        this.minNextBid = minNextBid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
}

