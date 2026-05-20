package com.team.backend.dto;

import jakarta.validation.constraints.Min;

public class AutoBidRequestDto {

    @Min(value = 1, message = "maxAmount must be >= 1")
    private double maxAmount;

    public AutoBidRequestDto() {
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }
}
