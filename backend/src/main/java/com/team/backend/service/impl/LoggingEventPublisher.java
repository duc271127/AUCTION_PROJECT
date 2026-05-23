package com.team.backend.service.impl;

import com.team.backend.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publishBidPlaced(UUID auctionId, UUID bidderId, double amount, UUID previousLeader, Instant timestamp) {
        log.info("EVENT BidPlaced auction={} bidder={} amount={} prevLeader={} ts={}", auctionId, bidderId, amount, previousLeader, timestamp);
    }

    @Override
    public void publishAuctionExtended(UUID auctionId, double newPrice, Instant newEndTime) {
        log.info("EVENT AuctionExtended auction={} newPrice={} newEndTime={}", auctionId, newPrice, newEndTime);
    }

    @Override
    public void publishAutoBidPlaced(UUID auctionId, UUID bidderId, double amount, UUID previousLeader, Instant timestamp) {
        log.info("EVENT AutoBidPlaced auction={} bidder={} amount={} prevLeader={} ts={}", auctionId, bidderId, amount, previousLeader, timestamp);
    }

    @Override
    public void publishAuctionClosed(UUID auctionId, UUID winnerId, double finalPrice, Instant timestamp) {
        log.info("EVENT AuctionClosed auction={} winner={} finalPrice={} ts={}", auctionId, winnerId, finalPrice, timestamp);
    }
}
