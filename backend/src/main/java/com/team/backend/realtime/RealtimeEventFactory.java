package com.team.backend.realtime;

import java.time.Instant;
import java.util.UUID;

public final class RealtimeEventFactory {

    private RealtimeEventFactory() {
    }

    public static RealtimeEvent bidPlaced(UUID auctionId,
                                          UUID bidderId,
                                          double currentPrice,
                                          Instant endTime) {
        RealtimeEvent event = new RealtimeEvent(
                RealtimeEventType.BID_PLACED,
                auctionId,
                bidderId,
                currentPrice,
                endTime
        );
        event.setMessage("Bid placed successfully");
        return event;
    }

    public static RealtimeEvent auctionClosed(UUID auctionId,
                                              UUID winnerId,
                                              double finalPrice,
                                              Instant endTime) {
        RealtimeEvent event = new RealtimeEvent(
                RealtimeEventType.AUCTION_CLOSED,
                auctionId,
                winnerId,
                finalPrice,
                endTime
        );
        event.setWinner(winnerId == null ? null : winnerId.toString());
        event.setMessage("Auction closed");
        return event;
    }

    public static RealtimeEvent auctionExtended(UUID auctionId,
                                                UUID bidderId,
                                                double currentPrice,
                                                Instant newEndTime) {
        RealtimeEvent event = new RealtimeEvent(
                RealtimeEventType.AUCTION_EXTENDED,
                auctionId,
                bidderId,
                currentPrice,
                newEndTime
        );
        event.setMessage("Auction extended");
        return event;
    }
}