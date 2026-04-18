package com.team.backend.service;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;

import java.util.List;
import java.util.UUID;

public interface AuctionService {
    Auction createAuction(Auction auction);
    Auction createAuction(AuctionCreateDto dto, UUID sellerId);
    Auction getAuction(UUID auctionId);
    List<Auction> listAuctions();
    List<Auction> listAuctionsByState(AuctionState state);
    Auction updateAuction(Auction auction);
    void closeAuction(UUID auctionId);
    void startAuction(UUID auctionId);
    void refreshStates(); // scan and update states based on time
    void validateAuctionOpenForBidding(UUID auctionId);

}
