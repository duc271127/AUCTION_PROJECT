package com.auction.server;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int highestBid = 0;
    private String highestBidder = "NONE";
    private final List<String> history = new ArrayList<>();

    public synchronized String placeBid(String bidder, int amount) {
        if (amount > highestBid) {
            highestBid = amount;
            highestBidder = bidder;

            String msg = "[NEW_BID] " + bidder + " -> " + amount + " | New highest bid";
            history.add(msg);
            return msg;
        } else {
            return "[FAIL] " + bidder + " -> " + amount + " | Bid too low";
        }
    }

    public synchronized List<String> getHistory() {
        return history;
    }
}