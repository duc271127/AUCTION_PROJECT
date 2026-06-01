package com.auction.client.dto.response;

public class BidPlacementResponse {
    private String auctionId;
    private String bidId;
    private String bidderId;
    private String bidderDisplay;
    private String leaderId;
    private String leaderName;
    private double currentPrice;
    private double minNextBid;
    private String state;
    private String endTime;

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getBidId() { return bidId; }
    public void setBidId(String bidId) { this.bidId = bidId; }
    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    public String getBidderDisplay() { return bidderDisplay; }
    public void setBidderDisplay(String bidderDisplay) { this.bidderDisplay = bidderDisplay; }
    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getMinNextBid() { return minNextBid; }
    public void setMinNextBid(double minNextBid) { this.minNextBid = minNextBid; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
