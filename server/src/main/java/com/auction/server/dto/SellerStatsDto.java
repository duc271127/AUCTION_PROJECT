package com.auction.server.dto;

public class SellerStatsDto {
    private long totalItems;
    private long pendingItems;
    private long approvedItems;
    private long rejectedItems;
    private long activeAuctions;
    private long completedAuctions;
    private double successRate;
    private double totalRevenue;
    private double averageSalePrice;

    public SellerStatsDto() {
    }

    public SellerStatsDto(long totalItems,
                          long pendingItems,
                          long approvedItems,
                          long rejectedItems,
                          long activeAuctions,
                          long completedAuctions,
                          double successRate,
                          double totalRevenue,
                          double averageSalePrice) {
        this.totalItems = totalItems;
        this.pendingItems = pendingItems;
        this.approvedItems = approvedItems;
        this.rejectedItems = rejectedItems;
        this.activeAuctions = activeAuctions;
        this.completedAuctions = completedAuctions;
        this.successRate = successRate;
        this.totalRevenue = totalRevenue;
        this.averageSalePrice = averageSalePrice;
    }

    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    public long getPendingItems() { return pendingItems; }
    public void setPendingItems(long pendingItems) { this.pendingItems = pendingItems; }

    public long getApprovedItems() { return approvedItems; }
    public void setApprovedItems(long approvedItems) { this.approvedItems = approvedItems; }

    public long getRejectedItems() { return rejectedItems; }
    public void setRejectedItems(long rejectedItems) { this.rejectedItems = rejectedItems; }

    public long getActiveAuctions() { return activeAuctions; }
    public void setActiveAuctions(long activeAuctions) { this.activeAuctions = activeAuctions; }

    public long getCompletedAuctions() { return completedAuctions; }
    public void setCompletedAuctions(long completedAuctions) { this.completedAuctions = completedAuctions; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getAverageSalePrice() { return averageSalePrice; }
    public void setAverageSalePrice(double averageSalePrice) { this.averageSalePrice = averageSalePrice; }
}

