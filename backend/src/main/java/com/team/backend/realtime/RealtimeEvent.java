package com.team.backend.realtime;

import java.time.LocalDateTime;

public class RealtimeEvent {

    private RealtimeEventType eventType;
    private Long auctionId;
    private Double currentPrice;
    private String currentLeader;
    private String winner;
    private String status;
    private String message;
    private LocalDateTime timestamp;

    public RealtimeEvent() {
    }

    public RealtimeEvent(RealtimeEventType eventType,
                         Long auctionId,
                         Double currentPrice,
                         String currentLeader,
                         String winner,
                         String status,
                         String message,
                         LocalDateTime timestamp) {
        this.eventType = eventType;
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.currentLeader = currentLeader;
        this.winner = winner;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public RealtimeEventType getEventType() {
        return eventType;
    }

    public void setEventType(RealtimeEventType eventType) {
        this.eventType = eventType;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}