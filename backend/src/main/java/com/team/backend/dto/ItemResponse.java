package com.team.backend.dto;

import java.util.UUID;

public class ItemResponse {
    private UUID id;
    private String productName;
    private String description;
    private String category;
    private double startingPrice;
    private Double reservePrice;
    private String status;
    private String startDate; // ISO-8601 string
    private String endDate;   // ISO-8601 string
    private String imagePath;

    // getters / setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public Double getReservePrice() { return reservePrice; }
    public void setReservePrice(Double reservePrice) { this.reservePrice = reservePrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
