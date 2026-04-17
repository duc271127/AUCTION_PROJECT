package com.team.backend.concurrent;

import com.team.backend.bidding.BidProcessingResult;

public class BidLockPrototype {

    public static void main(String[] args) throws InterruptedException {
        AuctionState auction = new AuctionState(1L, 100.0, "No Leader yet", "OPEN");
        ConcurrentBidProcessor processor = new ConcurrentBidProcessor();

        Runnable bidderA = () -> {
            BidProcessingResult result = processor.processBid(auction, "UserA", 120.0);
            System.out.println(Thread.currentThread().getName() + " -> " + result);
        };

        Runnable bidderB = () -> {
            BidProcessingResult result = processor.processBid(auction, "UserB", 130.0);
            System.out.println(Thread.currentThread().getName() + " -> " + result);
        };

        Thread threadA = new Thread(bidderA, "Bid-Thread-A");
        Thread threadB = new Thread(bidderB, "Bid-Thread-B");

        System.out.println("=== START PHASE 2 CONCURRENT BID TEST ===");
        System.out.printf("Initial state -> auctionId=%d, currentPrice=%.2f, leader=%s, status=%s%n",
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getCurrentLeader(),
                auction.getStatus());

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println("\\n=== FINAL AUCTION STATE ===");
        System.out.printf("auctionId=%d, currentPrice=%.2f, leader=%s, status=%s%n",
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getCurrentLeader(),
                auction.getStatus());
    }
}