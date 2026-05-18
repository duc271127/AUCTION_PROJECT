package com.team.backend.concurrent;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuctionRegistry {

    private final Map<Long, AuctionState> auctions = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSampleData() {
        auctions.put(1L, new AuctionState(
                1L,
                100.0,
                "No Leader yet",
                "OPEN",
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(30)
        ));
    }

    public AuctionState getAuction(Long auctionId) {
        return auctions.get(auctionId);
    }

    public Map<Long, AuctionState> getAuctions() {
        return auctions;
    }

    public void saveAuction(AuctionState auction) {
        auctions.put(auction.getAuctionId(), auction);
    }
}