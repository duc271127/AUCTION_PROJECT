package com.auction.client.dto.response;

public class SellerStatsResponse {
    private long totalItems;
    private long pendingItems;
    private long approvedItems;
    private long rejectedItems;
    private long activeAuctions;

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
}
