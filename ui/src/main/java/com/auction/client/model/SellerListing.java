package com.auction.client.model;

public class SellerListing {
    private final String productName;
    private final String category;
    private final String startingPrice;
    private final String status;
    private final String startDate;
    private final String endDate;

    public SellerListing (String productName, String category, String startingPrice, String status, String startDate, String endDate) {
        this.productName = productName;
        this.category = category;
        this.startingPrice = startingPrice;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public String getStartingPrice() {
        return startingPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

}