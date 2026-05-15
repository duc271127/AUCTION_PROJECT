package com.auction.client.model;

import java.util.UUID;

public class AdminApprovalItem {
    private UUID id;
    private UUID sellerId;

    private String productName;
    private String description;
    private String category;

    private double startingPrice;
    private Double reservePrice;

    private String status;
    private String imagePath;

    private String startDate;
    private String endDate;

    public AdminApprovalItem() {
    }

    public AdminApprovalItem(UUID id,
                             UUID sellerId,
                             String productName,
                             String description,
                             String category,
                             double startingPrice,
                             Double reservePrice,
                             String status,
                             String imagePath,
                             String startDate,
                             String endDate) {
        this.id = id;
        this.sellerId = sellerId;
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.status = status;
        this.imagePath = imagePath;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getItemId() {
        return id == null ? null : id.toString();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSubmittedDate() {
        return startDate;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Double getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(Double reservePrice) {
        this.reservePrice = reservePrice;
    }

    public String getStatus() {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}