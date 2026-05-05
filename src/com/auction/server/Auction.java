package com.auction.server;

public class Auction {
    private int highestBid = 0;
    private String highestBidder = "None";

    public synchronized boolean placeBid(String user, int amount) {
        if (amount > highestBid) {
            highestBid = amount;
            highestBidder = user;
            return true;
        }
        return false;
    }

    public synchronized int getHighestBid() {
        return highestBid;
    }

    public synchronized String getHighestBidder() {
        return highestBidder;
    }
}