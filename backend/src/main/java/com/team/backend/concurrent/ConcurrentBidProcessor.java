package com.team.backend.concurrent;

import com.team.backend.bidding.BidProcessingResult;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;

public class ConcurrentBidProcessor {

    private final AuctionLockManager auctionLockManager = new AuctionLockManager();

    public BidProcessingResult processBid(AuctionState auction, String bidderName, double bidAmount) {
        Lock lock = auctionLockManager.getLock(auction.getAuctionId());

        lock.lock();
        try {
            System.out.printf("[%s] %s acquired lock for auction %d at %s%n",
                    Thread.currentThread().getName(),
                    bidderName,
                    auction.getAuctionId(),
                    LocalDateTime.now());

            if (!"OPEN".equals(auction.getStatus())) {
                return new BidProcessingResult(
                        false,
                        "Auction is not open",
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

            auction.setCurrentPrice(bidAmount);
            auction.setCurrentLeader(bidderName);

            return new BidProcessingResult(
                    true,
                    "Bid accepted",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader()
            );
        } finally {
            System.out.printf("[%s] released lock for auction %d%n",
                    Thread.currentThread().getName(),
                    auction.getAuctionId());
            lock.unlock();
        }
    }
}