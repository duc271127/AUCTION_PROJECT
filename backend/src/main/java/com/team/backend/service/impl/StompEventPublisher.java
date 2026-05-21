package com.team.backend.service.impl;

import com.team.backend.dto.AuctionRealtimeEvent;
import com.team.backend.service.AuctionHelper;
import com.team.backend.service.EventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class StompEventPublisher implements EventPublisher {

    private final SimpMessagingTemplate template;
    private final AuctionHelper auctionHelper;

    public StompEventPublisher(SimpMessagingTemplate template, AuctionHelper auctionHelper) {
        this.template = template;
        this.auctionHelper = auctionHelper;
    }

    @Override
    public void publishBidPlaced(UUID auctionId, UUID bidderId, double amount, UUID previousLeader, Instant timestamp) {
        AuctionRealtimeEvent e = new AuctionRealtimeEvent();
        e.auctionId = auctionId;
        e.currentPrice = amount;
        e.leaderName = auctionHelper.lookupUserName(bidderId);
        e.remainingSeconds = auctionHelper.computeRemainingSeconds(auctionId);
        e.eventId = UUID.randomUUID().toString();
        e.latestBid = auctionHelper.toBidHistoryItem(bidderId, amount, timestamp);
        template.convertAndSend("/topic/auction." + auctionId, e);
    }

    @Override
    public void publishAuctionExtended(UUID auctionId, double newPrice, Instant newEndTime) {
        AuctionRealtimeEvent e = new AuctionRealtimeEvent();
        e.auctionId = auctionId;
        e.currentPrice = newPrice;
        e.leaderName = null;
        e.remainingSeconds = auctionHelper.computeRemainingSeconds(auctionId);
        e.eventId = UUID.randomUUID().toString();
        template.convertAndSend("/topic/auction." + auctionId, e);
    }

    @Override
    public void publishAutoBidPlaced(UUID auctionId, UUID bidderId, double amount, Instant timestamp) {
        AuctionRealtimeEvent e = new AuctionRealtimeEvent();
        e.auctionId = auctionId;
        e.currentPrice = amount;
        e.leaderName = auctionHelper.lookupUserName(bidderId);
        e.remainingSeconds = auctionHelper.computeRemainingSeconds(auctionId);
        e.eventId = UUID.randomUUID().toString();
        e.latestBid = auctionHelper.toBidHistoryItem(bidderId, amount, timestamp);
        template.convertAndSend("/topic/auction." + auctionId, e);
    }
}
