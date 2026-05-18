package com.team.backend.bidding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BidRequest {

    @NotBlank(message = "Bidder name must not be blank")
    private String bidderName;

    @NotNull(message = "Bid amount must not be null")
    @Min(value = 1, message = "Bid amount must be greater than 0")
    private Double bidAmount;

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }
}