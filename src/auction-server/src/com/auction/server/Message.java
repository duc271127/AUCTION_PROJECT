package com.auction.server;

public class Message {
    public String type;
    public String bidder;
    public int amount;

    public Message() {}

    public Message(String type, String bidder, int amount) {
        this.type = type;
        this.bidder = bidder;
        this.amount = amount;
    }
}