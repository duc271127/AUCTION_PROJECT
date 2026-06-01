package com.auction.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoBidRequestDto {

    @Min(value = 1, message = "maxAmount must be >= 1")
    private double maxAmount;

    @Min(value = 1, message = "bidStep must be >= 1")
    private double bidStep = 1.0;

    public AutoBidRequestDto() {
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getBidStep() {
        return bidStep;
    }

    public void setBidStep(double bidStep) {
        this.bidStep = bidStep;
    }
}

