package com.team.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class BidRequestDto {

    @NotNull
    public UUID bidderId;

    @Positive
    public double amount;
}
