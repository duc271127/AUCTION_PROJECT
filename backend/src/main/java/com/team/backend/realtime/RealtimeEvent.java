package com.team.backend.realtime;

import java.time.Instant;
import java.util.UUID;

public class RealtimeEvent {

    private RealtimeEventType eventType;
    private UUID auctionId;
    private UUID bidderId;
    private Double currentPrice;
    private String currentLeader;
    private String winner;
    private String status;
    private String message;
    private Instant endTime;
    private Instant timestamp;

    public RealtimeEvent() {
    }

    public RealtimeEvent(RealtimeEventType eventType,
                         UUID auctionId,
                         UUID bidderId,
                         Double currentPrice,
                         Instant endTime) {
        this.eventType = eventType;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.timestamp = Instant.now();
        this.status = eventType == null ? null : eventType.name();
        this.message = "Bid placed successfully";
    }

    public RealtimeEventType getEventType() {
        return eventType;
    }

    public void setEventType(RealtimeEventType eventType) {
        this.eventType = eventType;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}