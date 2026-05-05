package com.team.backend.service;

import com.team.backend.entity.Auction;
import com.team.backend.entity.Item;

import java.time.Instant;
import java.util.UUID;

public interface AdminService {
    Item approveItem(UUID itemId, UUID adminId);
    Auction createAuctionForItem(UUID itemId, Instant start, Instant end, UUID adminId, double startingPrice, Double reservePrice);
}
