package com.team.backend.service;

import com.team.backend.dto.PendingItemDto;
import com.team.backend.dto.AdminWalletActivityDto;
import com.team.backend.dto.AdminNotificationDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.Item;
import com.team.backend.dto.AdminStatsDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminService {
    Item approveItem(UUID itemId, UUID adminId);
    Item rejectItem(UUID itemId, UUID adminId);
    void deleteItem(UUID itemId);
    List<PendingItemDto> listReportedItems();
    AdminStatsDto getStats();
    List<AdminWalletActivityDto> getRecentWalletActivity(int limit);
    List<AdminNotificationDto> getRecentNotifications(int limit);
    Auction createAuctionForItem(UUID itemId, Instant start, Instant end, UUID adminId, double startingPrice, Double reservePrice);
    List<PendingItemDto> listPendingItems();
}
