package com.auction.server.dto;

import java.util.UUID;

public class PlaceBidRequest {
    public UUID auctionId;
    public UUID bidderId;
    public double amount;
}

