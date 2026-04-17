package com.team.backend.bidding;

public class BidProcessingResult {

    private final boolean accepted;
    private final String message;
    private final double currentPrice;
    private final String currentLeader;

    public BidProcessingResult(boolean accepted, String message, double currentPrice, String currentLeader) {
        this.accepted = accepted;
        this.message = message;
        this.currentPrice = currentPrice;
        this.currentLeader = currentLeader;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getMessage() {
        return message;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    @Override
    public String toString() {
        return "BidProcessingResult{" +
                "accepted=" + accepted +
                ", message='" + message + '\'' +
                ", currentPrice=" + currentPrice +
                ", currentLeader='" + currentLeader + '\'' +
                '}';
    }
}