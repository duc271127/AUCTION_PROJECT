package com.auction.client.model;

public class AuctionItem {
    private String id;
    private String name;
    private String imagePath;
    private String currentBid;
    private String timeLeft;
    private String status;

    public AuctionItem(String id, String name, String imagePath, String currentBid, String timeLeft, String status) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.currentBid = currentBid;
        this.timeLeft = timeLeft;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getCurrentBid() {
        return currentBid;
    }

    public String getTimeLeft() {
        return timeLeft;
    }

    public String getStatus() {
        return status;
    }
}