package com.team.backend.dto;

public class AdminStatsDto {
    private long totalUsers;
    private long activeSellers;
    private long totalAuctions;
    private long activeAuctions;
    private long closedAuctions;
    private long newSellersThisMonth;
    private double auctionSuccessRate;
    private double revenue;

    public AdminStatsDto() {
    }

    public AdminStatsDto(long totalUsers,
                         long activeSellers,
                         long totalAuctions,
                         long activeAuctions,
                         long closedAuctions,
                         long newSellersThisMonth,
                         double auctionSuccessRate,
                         double revenue) {
        this.totalUsers = totalUsers;
        this.activeSellers = activeSellers;
        this.totalAuctions = totalAuctions;
        this.activeAuctions = activeAuctions;
        this.closedAuctions = closedAuctions;
        this.newSellersThisMonth = newSellersThisMonth;
        this.auctionSuccessRate = auctionSuccessRate;
        this.revenue = revenue;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveSellers() { return activeSellers; }
    public void setActiveSellers(long activeSellers) { this.activeSellers = activeSellers; }

    public long getTotalAuctions() { return totalAuctions; }
    public void setTotalAuctions(long totalAuctions) { this.totalAuctions = totalAuctions; }

    public long getActiveAuctions() { return activeAuctions; }
    public void setActiveAuctions(long activeAuctions) { this.activeAuctions = activeAuctions; }

    public long getClosedAuctions() { return closedAuctions; }
    public void setClosedAuctions(long closedAuctions) { this.closedAuctions = closedAuctions; }

    public long getNewSellersThisMonth() { return newSellersThisMonth; }
    public void setNewSellersThisMonth(long newSellersThisMonth) { this.newSellersThisMonth = newSellersThisMonth; }

    public double getAuctionSuccessRate() { return auctionSuccessRate; }
    public void setAuctionSuccessRate(double auctionSuccessRate) { this.auctionSuccessRate = auctionSuccessRate; }

    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
}
