package com.auction.client.model;
import com.auction.client.util.DateTimeDisplayHelper;
import java.util.UUID;
import java.util.List;
import java.util.Locale;

public class SellerListing {
    private UUID id;
    private UUID sellerId;
    private String productName;
    private String description;
    private String category;
    private String startingPrice;
    private String reservePrice;
    private String status;
    private String startDate;
    private String endDate;
    private String imagePath;
    private List<String> imageUrls;
    private String sku;
    private Integer quantity;
    private boolean deletable = true;
    private String deleteBlockedReason;
    private boolean editable = true;
    private String editBlockedReason;

    public SellerListing() {
    }

    public SellerListing(UUID id, UUID sellerId, String productName, String description, String category,
                         String startingPrice, String reservePrice,
                         String status, String startDate, String endDate, String imagePath, List<String> imageUrls,
                         String sku, Integer quantity, boolean deletable, String deleteBlockedReason) {
        this.id = id;
        this.sellerId = sellerId;
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imagePath = imagePath;
        this.imageUrls = imageUrls;
        this.sku = sku;
        this.quantity = quantity;
        this.deletable = deletable;
        this.deleteBlockedReason = deleteBlockedReason;
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

    public String getDescription() {
        return description;
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

    public String getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(String startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(String reservePrice) {
        this.reservePrice = reservePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public boolean isDeletable() { return deletable; }
    public void setDeletable(boolean deletable) { this.deletable = deletable; }

    public String getDeleteBlockedReason() { return deleteBlockedReason; }
    public void setDeleteBlockedReason(String deleteBlockedReason) { this.deleteBlockedReason = deleteBlockedReason; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public String getEditBlockedReason() { return editBlockedReason; }
    public void setEditBlockedReason(String editBlockedReason) { this.editBlockedReason = editBlockedReason; }

    public String getQuantityText() {
        return quantity == null ? "0" : String.valueOf(quantity);
    }

    public String getDeletePermissionText() {
        return deletable ? "Yes" : "No";
    }

    public String getStartingPriceText() {
        return formatMoney(startingPrice);
    }

    public String getReservePriceText() {
        if (reservePrice == null || reservePrice.isBlank()) {
            return "-";
        }

        try {
            double value = Double.parseDouble(reservePrice);
            return value <= 0 ? "-" : formatMoney(reservePrice);
        } catch (NumberFormatException e) {
            return reservePrice;
        }
    }

    public String getStartDateText() {
        return formatDate(startDate);
    }

    public String getEndDateText() {
        return formatDate(endDate);
    }

    private String formatMoney(String value) {
        if (value == null || value.isBlank()) {
            return "USD 0";
        }

        try {
            return "USD " + String.format(Locale.US, "%,.0f", Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String formatDate(String value) {
        return DateTimeDisplayHelper.formatDateTime(value, "-");
    }
}
