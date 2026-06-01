package com.auction.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BidRequestDto {

    public UUID bidderId;

    @Positive
    public double amount;
}

