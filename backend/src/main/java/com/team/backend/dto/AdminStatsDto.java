package com.team.backend.dto;

public class AdminStatsDto {
    private long totalUsers;
    private long activeSellers;
    private long totalAuctions;
    private double revenue;

    public AdminStatsDto() {
    }

    public AdminStatsDto(long totalUsers, long activeSellers, long totalAuctions, double revenue) {
        this.totalUsers = totalUsers;
        this.activeSellers = activeSellers;
        this.totalAuctions = totalAuctions;
        this.revenue = revenue;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveSellers() { return activeSellers; }
    public void setActiveSellers(long activeSellers) { this.activeSellers = activeSellers; }

    public long getTotalAuctions() { return totalAuctions; }
    public void setTotalAuctions(long totalAuctions) { this.totalAuctions = totalAuctions; }

    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
}