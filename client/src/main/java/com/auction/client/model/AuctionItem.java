package com.auction.client.model;

public class AuctionItem {
    private String id;
    private String name;
    private String imagePath;
    private String currentBid;
    private String timeLeft;
    private String status;
    private String createdAt;
    private String endTime;
    private long favoriteCount;

    public AuctionItem(String id, String name, String imagePath, String currentBid, String timeLeft, String status) {
        this(id, name, imagePath, currentBid, timeLeft, status, null, null, 0);
    }

    public AuctionItem(String id,
                       String name,
                       String imagePath,
                       String currentBid,
                       String timeLeft,
                       String status,
                       String createdAt,
                       String endTime) {
        this(id, name, imagePath, currentBid, timeLeft, status, createdAt, endTime, 0);
    }

    public AuctionItem(String id,
                       String name,
                       String imagePath,
                       String currentBid,
                       String timeLeft,
                       String status,
                       String createdAt,
                       String endTime,
                       long favoriteCount) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath;
        this.currentBid = currentBid;
        this.timeLeft = timeLeft;
        this.status = status;
        this.createdAt = createdAt;
        this.endTime = endTime;
        this.favoriteCount = favoriteCount;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public String getEndTime() {
        return endTime;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }
}
