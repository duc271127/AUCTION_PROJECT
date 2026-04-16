package com.team.backend.service;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;

import java.util.List;
import java.util.UUID;

public interface AuctionService {
    // existing methods
    Auction createAuction(Auction auction);
    Auction getAuction(UUID auctionId);
    List<Auction> listAuctions();
    Auction updateAuction(Auction auction);
    void closeAuction(UUID auctionId);

    // new method: create from DTO with sellerId (Phase2)
    Auction createAuction(AuctionCreateDto dto, UUID sellerId);

    // optional helper
    List<Auction> listAuctionsByState(AuctionState state);
}
