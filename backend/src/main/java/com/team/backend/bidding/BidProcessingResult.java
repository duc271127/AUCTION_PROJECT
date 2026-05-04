package com.team.backend.bidding;

import com.team.backend.realtime.RealtimeEvent;

import java.util.ArrayList;
import java.util.List;

public class BidProcessingResult {

    private final boolean accepted;
    private final String message;
    private final double currentPrice;
    private final String currentLeader;
    private final List<RealtimeEvent> events;

    public BidProcessingResult(boolean accepted,
                               String message,
                               double currentPrice,
                               String currentLeader) {
        this(accepted, message, currentPrice, currentLeader, new ArrayList<>());
    }

    public BidProcessingResult(boolean accepted,
                               String message,
                               double currentPrice,
                               String currentLeader,
                               List<RealtimeEvent> events) {
        this.accepted = accepted;
        this.message = message;
        this.currentPrice = currentPrice;
        this.currentLeader = currentLeader;
        this.events = events;
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

    public List<RealtimeEvent> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return "BidProcessingResult{" +
                "accepted=" + accepted +
                ", message='" + message + '\'' +
                ", currentPrice=" + currentPrice +
                ", currentLeader='" + currentLeader + '\'' +
                ", events=" + events +
                '}';
    }
}