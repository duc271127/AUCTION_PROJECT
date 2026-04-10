package com.auction.client.model;

public class AdminApprovalItem {
    private final String productName;
    private final String sellerName;
    private final String category;
    private final String submittedDate;
    private String status;

    public AdminApprovalItem(String productName, String sellerName, String category, String submittedDate, String status) {
        this.productName = productName;
        this.sellerName = sellerName;
        this.category = category;
        this.submittedDate = submittedDate;
        this.status = status;
    }

    public String getProductName() {
        return productName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getCategory() {
        return category;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}