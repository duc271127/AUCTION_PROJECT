package com.auction.server.service;

import com.auction.server.dto.AuctionCreateDto;
import com.auction.server.dto.AuctionDetailResponse;
import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AuctionService {
    Auction createAuction(Auction auction);
    Auction createAuction(AuctionCreateDto dto, UUID sellerId);
    Auction createAuction(AuctionCreateDto dto, UUID sellerId, UUID actorId);
    Auction getAuction(UUID auctionId);
    List<Auction> listAuctions();
    List<Auction> listAuctionsByState(AuctionState state);
    List<Auction> listWonAuctions(UUID winnerId);
    Auction updateAuction(Auction auction);
    void acceptAuction(UUID auctionId);
    void closeAuction(UUID auctionId);
    void closeAuction(UUID auctionId, String reason);
    void rejectAuction(UUID auctionId, String reason);
    void deleteAuction(UUID auctionId);
    void purgeAuction(UUID auctionId);
    void startAuction(UUID auctionId);
    void refreshStates();
    void validateAuctionOpenForBidding(UUID auctionId);
    AuctionDetailResponse getDetail(UUID auctionId);
    AuctionDetailResponse getDetail(UUID auctionId, boolean incrementView);
    Page<Auction> searchCatalog(String category, String q, AuctionState state, Pageable pageable);
    Page<Auction> searchTrendingCatalog(String category, String q, AuctionState state, Pageable pageable);
    Page<Auction> searchPersonalizedCatalog(UUID userId, String category, String q, AuctionState state, Pageable pageable);
    void incrementView(UUID auctionId);
}

