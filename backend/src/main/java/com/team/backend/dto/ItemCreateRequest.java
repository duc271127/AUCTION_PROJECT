package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class ItemCreateRequest {
    private String productName;
    private String description;
    private String category;
    private Double startingPrice;
    private Double reservePrice;
    private String status; // optional initial status
    private Instant startDate; // frontend có thể gửi ISO string -> map thành Instant
    private Instant endDate;
    private String imagePath;

    // getters / setters
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }

    public Double getReservePrice() { return reservePrice; }
    public void setReservePrice(Double reservePrice) { this.reservePrice = reservePrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }

    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
