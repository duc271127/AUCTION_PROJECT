package com.team.backend.realtime;

import com.team.backend.concurrent.AuctionState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RealtimeEventFactory {

    public RealtimeEvent buildBidPlacedEvent(AuctionState auction) {
        return new RealtimeEvent(
                RealtimeEventType.BID_PLACED,
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getCurrentLeader(),
                auction.getWinner(),
                auction.getStatus(),
                "New bid accepted",
                LocalDateTime.now()
        );
    }

    public RealtimeEvent buildLeaderChangedEvent(AuctionState auction) {
        return new RealtimeEvent(
                RealtimeEventType.LEADER_CHANGED,
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getCurrentLeader(),
                auction.getWinner(),
                auction.getStatus(),
                "Leader changed",
                LocalDateTime.now()
        );
    }

    public RealtimeEvent buildAuctionFinishedEvent(AuctionState auction) {
        return new RealtimeEvent(
                RealtimeEventType.AUCTION_FINISHED,
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getCurrentLeader(),
                auction.getWinner(),
                auction.getStatus(),
                "Auction finished",
                LocalDateTime.now()
        );
    }
}