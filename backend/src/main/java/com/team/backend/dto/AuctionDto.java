package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;

public class AuctionDto {
    public UUID id;
    public UUID itemId;
    public String itemName;
    public double currentPrice;
    public UUID leaderId;
    public Instant startTime;
    public Instant endTime;
    public String state;
}
