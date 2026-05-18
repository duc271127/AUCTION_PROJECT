package com.team.backend.concurrent;

import com.team.backend.bidding.BidProcessingResult;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Service
public class ConcurrentBidProcessor {

    private final AuctionLockManager auctionLockManager;
    private final RealtimeEventFactory realtimeEventFactory;

    public ConcurrentBidProcessor(AuctionLockManager auctionLockManager,
                                  RealtimeEventFactory realtimeEventFactory) {
        this.auctionLockManager = auctionLockManager;
        this.realtimeEventFactory = realtimeEventFactory;
    }

    public BidProcessingResult processBid(AuctionState auction, String bidderName, double bidAmount) {
        Lock lock = auctionLockManager.getLock(auction.getAuctionId());

        lock.lock();
        try {
            System.out.printf("[%s] %s acquired lock for auction %d at %s%n",
                    Thread.currentThread().getName(),
                    bidderName,
                    auction.getAuctionId(),
                    LocalDateTime.now());

            BidProcessingResult validationResult = validateBid(auction, bidAmount);
            if (validationResult != null) {
                return validationResult;
            }

            String oldLeader = auction.getCurrentLeader();

            applyBid(auction, bidderName, bidAmount);

            List<RealtimeEvent> events = new ArrayList<>();
            events.add(realtimeEventFactory.buildBidPlacedEvent(auction));

            if (oldLeader == null || !oldLeader.equals(auction.getCurrentLeader())) {
                events.add(realtimeEventFactory.buildLeaderChangedEvent(auction));
            }

            return new BidProcessingResult(
                    true,
                    "Bid accepted",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader(),
                    events
            );
        } finally {
            System.out.printf("[%s] released lock for auction %d%n",
                    Thread.currentThread().getName(),
                    auction.getAuctionId());
            lock.unlock();
        }
    }

    private BidProcessingResult validateBid(AuctionState auction, double bidAmount) {
        if (!auction.isOpen()) {
            return new BidProcessingResult(
                    false,
                    "Auction is not open",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader()
            );
        }

        if (auction.getEndTime() != null && LocalDateTime.now().isAfter(auction.getEndTime())) {
            auction.setStatus("CLOSED");
            auction.setWinner(auction.getCurrentLeader());

            return new BidProcessingResult(
                    false,
                    "Auction has already ended",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader()
            );
        }

        if (bidAmount <= auction.getCurrentPrice()) {
            return new BidProcessingResult(
                    false,
                    "Bid amount must be greater than current price",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader()
            );
        }

        return null;
    }

    private void applyBid(AuctionState auction, String bidderName, double bidAmount) {
        auction.setCurrentPrice(bidAmount);
        auction.setCurrentLeader(bidderName);
        auction.addBidRecord(new BidRecord(bidderName, bidAmount, LocalDateTime.now()));
    }

    public BidProcessingResult closeAuction(AuctionState auction) {
        Lock lock = auctionLockManager.getLock(auction.getAuctionId());

        lock.lock();
        try {
            if (auction.isClosed()) {
                return new BidProcessingResult(
                        false,
                        "Auction already closed",
                        auction.getCurrentPrice(),
                        auction.getCurrentLeader()
                );
            }

            auction.setStatus("CLOSED");
            auction.setWinner(auction.getCurrentLeader());

            List<RealtimeEvent> events = new ArrayList<>();
            events.add(realtimeEventFactory.buildAuctionFinishedEvent(auction));

            return new BidProcessingResult(
                    true,
                    "Auction closed successfully",
                    auction.getCurrentPrice(),
                    auction.getWinner(),
                    events
            );
        } finally {
            lock.unlock();
        }
    }
}