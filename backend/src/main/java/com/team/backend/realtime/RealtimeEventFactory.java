package com.team.backend.realtime;

import com.team.backend.dto.BidHistoryDto;

import java.time.Instant;
import java.util.UUID;

public final class RealtimeEventFactory {

    private RealtimeEventFactory() {
    }

    public static RealtimeEvent bidPlaced(UUID auctionId,
                                          UUID bidId,
                                          UUID bidderId,
                                          String bidderName,
                                          UUID leaderId,
                                          String leaderName,
                                          double currentPrice,
                                          String state,
                                          long remainingSeconds,
                                          Instant endTime,
                                          BidHistoryDto latestBid,
                                          String message) {
        RealtimeEvent event = baseEvent(RealtimeEventType.BID_PLACED, auctionId, bidderId, bidderName, leaderId, leaderName, currentPrice, state, remainingSeconds, endTime, latestBid);
        event.setBidId(bidId);
        event.setMessage(message);
        return event;
    }

    public static RealtimeEvent leaderChanged(UUID auctionId,
                                              UUID bidderId,
                                              String bidderName,
                                              UUID leaderId,
                                              String leaderName,
                                              double currentPrice,
                                              String state,
                                              long remainingSeconds,
                                              Instant endTime,
                                              String message) {
        RealtimeEvent event = baseEvent(RealtimeEventType.LEADER_CHANGED, auctionId, bidderId, bidderName, leaderId, leaderName, currentPrice, state, remainingSeconds, endTime, null);
        event.setMessage(message);
        return event;
    }

    public static RealtimeEvent auctionExtended(UUID auctionId,
                                                UUID leaderId,
                                                String leaderName,
                                                double currentPrice,
                                                String state,
                                                long remainingSeconds,
                                                Instant endTime,
                                                String message) {
        RealtimeEvent event = baseEvent(RealtimeEventType.AUCTION_EXTENDED, auctionId, null, null, leaderId, leaderName, currentPrice, state, remainingSeconds, endTime, null);
        event.setMessage(message);
        return event;
    }

    public static RealtimeEvent auctionClosed(UUID auctionId,
                                              UUID leaderId,
                                              String leaderName,
                                              double currentPrice,
                                              String state,
                                              long remainingSeconds,
                                              Instant endTime,
                                              String message) {
        RealtimeEvent event = baseEvent(RealtimeEventType.AUCTION_CLOSED, auctionId, null, null, leaderId, leaderName, currentPrice, state, remainingSeconds, endTime, null);
        event.setMessage(message);
        return event;
    }

    private static RealtimeEvent baseEvent(RealtimeEventType type,
                                           UUID auctionId,
                                           UUID bidderId,
                                           String bidderName,
                                           UUID leaderId,
                                           String leaderName,
                                           double currentPrice,
                                           String state,
                                           long remainingSeconds,
                                           Instant endTime,
                                           BidHistoryDto latestBid) {
        RealtimeEvent event = new RealtimeEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(type);
        event.setAuctionId(auctionId);
        event.setBidderId(bidderId);
        event.setBidderName(bidderName);
        event.setLeaderId(leaderId);
        event.setLeaderName(leaderName);
        event.setCurrentPrice(currentPrice);
        event.setState(state);
        event.setRemainingSeconds(remainingSeconds);
        event.setEndTime(endTime);
        event.setTimestamp(Instant.now());
        event.setLatestBid(latestBid);
        return event;
    }
}
