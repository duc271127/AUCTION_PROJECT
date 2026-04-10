package com.team.backend.service;

import com.team.backend.entity.Auction;
import java.util.List;
import java.util.UUID;

public interface AuctionService {
    Auction createAuction(Auction auction);
    Auction getAuction(UUID auctionId);
    List<Auction> listAuctions();
    Auction updateAuction(Auction auction);
    void closeAuction(UUID auctionId);
}
