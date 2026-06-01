package com.auction.server.service;

import com.auction.server.dto.PendingItemDto;
import com.auction.server.dto.AdminWalletActivityDto;
import com.auction.server.dto.AdminNotificationDto;
import com.auction.server.entity.Auction;
import com.auction.server.entity.Item;
import com.auction.server.dto.AdminStatsDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminService {
    Item approveItem(UUID itemId, UUID adminId);
    Item rejectItem(UUID itemId, UUID adminId);
    void deleteItem(UUID itemId);
    void acceptAuction(UUID auctionId, UUID adminId);
    void rejectAuction(UUID auctionId, UUID adminId);
    void deleteAuction(UUID auctionId, UUID adminId);
    void purgeAuction(UUID auctionId, UUID adminId);
    List<PendingItemDto> listReportedItems();
    AdminStatsDto getStats();
    List<AdminWalletActivityDto> getRecentWalletActivity(int limit);
    List<AdminNotificationDto> getRecentNotifications(int limit);
    Auction createAuctionForItem(UUID itemId, Instant start, Instant end, UUID adminId, double startingPrice, Double reservePrice);
    Auction approveAndCreateAuction(UUID itemId, Instant start, Instant end, UUID adminId, double startingPrice, Double reservePrice);
    List<PendingItemDto> listPendingItems();
}

