package com.auction.server;

public class Message {
    public String type;
    public String user;
    public int amount;
    public String content;

    public Message(String type, String user, int amount, String content) {
        this.type = type;
        this.user = user;
        this.amount = amount;
        this.content = content;
    }
}