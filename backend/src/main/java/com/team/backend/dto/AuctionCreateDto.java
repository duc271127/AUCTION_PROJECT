package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

/**
 * Backward compatible AuctionCreateDto.
 * If itemId is provided, server will use existing item.
 * Otherwise itemName + startPrice will be used to create a new item.
 */
public class AuctionCreateDto {
    // backward fields (Phase1)
    public String itemName;
    public String itemDescription;
    public double startPrice;

    // new Phase2 field
    public UUID itemId;

    @NotNull
    public Instant startTime;

    @NotNull
    public Instant endTime;
}
