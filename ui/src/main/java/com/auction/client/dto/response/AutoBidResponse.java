package com.auction.client.dto.response;

public class AutoBidResponse {
    private String autoBidId;
    private String auctionId;
    private String bidderId;
    private String bidderName;
    private double maxAmount;
    private boolean active;
    private String auctionState;
    private String endTime;
    private String createdAt;
    private String updatedAt;

    public String getAutoBidId() { return autoBidId; }
    public void setAutoBidId(String autoBidId) { this.autoBidId = autoBidId; }
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getAuctionState() { return auctionState; }
    public void setAuctionState(String auctionState) { this.auctionState = auctionState; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
