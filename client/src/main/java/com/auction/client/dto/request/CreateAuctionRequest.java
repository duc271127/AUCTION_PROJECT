package com.auction.client.dto.request;

public class CreateAuctionRequest {
    private String startTime;
    private String endTime;
    private Double startingPrice;
    private Double reservePrice;

    public CreateAuctionRequest() {
    }

    public CreateAuctionRequest(String startTime, String endTime,
                                Double startingPrice, Double reservePrice) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(Double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Double getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(Double reservePrice) {
        this.reservePrice = reservePrice;
    }
}