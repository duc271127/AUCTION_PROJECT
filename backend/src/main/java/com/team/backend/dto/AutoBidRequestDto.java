package com.team.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class AutoBidRequestDto {

    @NotNull
    public UUID bidderId;

    @Positive
    public double maxAmount;
}