package com.team.backend.dto;

import java.time.Instant;

public class AuctionCreateDto {
    public String itemName;
    public String itemDescription;
    public double startPrice;
    public Instant startTime;
    public Instant endTime;
}
