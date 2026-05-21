package com.auction.client.dto.response;

public class AdminStatsResponse {
    private long totalUsers;
    private long activeSellers;
    private long totalAuctions;
    private double revenue;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveSellers() { return activeSellers; }
    public void setActiveSellers(long activeSellers) { this.activeSellers = activeSellers; }

    public long getTotalAuctions() { return totalAuctions; }
    public void setTotalAuctions(long totalAuctions) { this.totalAuctions = totalAuctions; }

    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
}
