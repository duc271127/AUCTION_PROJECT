package com.team.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
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
