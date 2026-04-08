package com.team.backend.concurrent;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Prototype cho Phase 1 - HARDCORE.
 * Mục tiêu:
 * - mô phỏng 2 thread cùng bid vào 1 auction
 * - dùng lock để đảm bảo chỉ một thread cập nhật state tại một thời điểm
 * - in log để dễ giải thích trong report hoặc code walkthrough
 */
public class BidLockPrototype {

    public static void main(String[] args) throws InterruptedException {
        AuctionState auction = new AuctionState(1L, 100.0, "No leader yet");

        Runnable bidderA = () -> placeBid(auction, "UserA", 120.0);
        Runnable bidderB = () -> placeBid(auction, "UserB", 130.0);

        Thread threadA = new Thread(bidderA, "Bid-Thread-A");
        Thread threadB = new Thread(bidderB, "Bid-Thread-B");

        System.out.println("=== START CONCURRENT BIDDING PROTOTYPE ===");
        System.out.printf("Initial state -> auctionId=%d, currentPrice=%.2f, leader=%s%n",
                auction.getAuctionId(), auction.getCurrentPrice(), auction.getCurrentLeader());
        System.out.println();

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println();
        System.out.println("=== FINAL AUCTION STATE ===");
        System.out.printf("auctionId=%d, currentPrice=%.2f, leader=%s%n",
                auction.getAuctionId(), auction.getCurrentPrice(), auction.getCurrentLeader());
        System.out.println("=== END PROTOTYPE ===");
    }

    private static void placeBid(AuctionState auction, String bidderName, double bidAmount) {
        System.out.printf("[%s] %s is trying to bid %.2f at %s%n",
                Thread.currentThread().getName(), bidderName, bidAmount, LocalDateTime.now());

        auction.getLock().lock();
        try {
            System.out.printf("[%s] %s acquired lock. Current price before validation: %.2f%n",
                    Thread.currentThread().getName(), bidderName, auction.getCurrentPrice());

            // Mô phỏng xử lý nghiệp vụ
            sleep(800);

            if (bidAmount <= auction.getCurrentPrice()) {
                System.out.printf("[%s] %s bid rejected. %.2f is not greater than current price %.2f%n",
                        Thread.currentThread().getName(), bidderName, bidAmount, auction.getCurrentPrice());
                return;
            }

            auction.setCurrentPrice(bidAmount);
            auction.setCurrentLeader(bidderName);

            System.out.printf("[%s] %s bid accepted. New current price = %.2f, leader = %s%n",
                    Thread.currentThread().getName(), bidderName,
                    auction.getCurrentPrice(), auction.getCurrentLeader());

        } finally {
            System.out.printf("[%s] %s released lock.%n", Thread.currentThread().getName(), bidderName);
            auction.getLock().unlock();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    private static class AuctionState {
        private final Long auctionId;
        private double currentPrice;
        private String currentLeader;
        private final ReentrantLock lock = new ReentrantLock();

        public AuctionState(Long auctionId, double currentPrice, String currentLeader) {
            this.auctionId = auctionId;
            this.currentPrice = currentPrice;
            this.currentLeader = currentLeader;
        }

        public Long getAuctionId() {
            return auctionId;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
        }

        public String getCurrentLeader() {
            return currentLeader;
        }

        public void setCurrentLeader(String currentLeader) {
            this.currentLeader = currentLeader;
        }

        public ReentrantLock getLock() {
            return lock;
        }
    }
}
