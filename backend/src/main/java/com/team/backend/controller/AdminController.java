package com.team.backend.controller;

import com.team.backend.dto.AdminStatsDto;
import com.team.backend.dto.AdminWalletActivityDto;
import com.team.backend.dto.AdminNotificationDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.CreateAuctionRequest;
import com.team.backend.dto.PendingItemDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.Item;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.service.AdminService;
import com.team.backend.service.UserService;
import com.team.backend.util.AuctionImageResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @PostMapping("/items/{itemId}/approve")
    public ResponseEntity<Item> approveItem(@PathVariable UUID itemId) {
        UUID adminId = resolveCurrentUserId();
        Item approved = adminService.approveItem(itemId, adminId);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/items/{itemId}/create-auction")
    public ResponseEntity<AuctionDto> createAuction(
            @PathVariable UUID itemId,
            @RequestBody CreateAuctionRequest req) {

        UUID adminId = resolveCurrentUserId();

        Auction auction = adminService.createAuctionForItem(
                itemId,
                req.getStartTime(),
                req.getEndTime(),
                adminId,
                req.getStartingPrice() == null ? 0.0 : req.getStartingPrice(),
                req.getReservePrice()
        );

        return ResponseEntity.ok(toAuctionDto(auction));
    }

    @PostMapping("/items/{itemId}/approve-and-create-auction")
    public ResponseEntity<AuctionDto> approveAndCreateAuction(
            @PathVariable UUID itemId,
            @RequestBody CreateAuctionRequest req) {

        UUID adminId = resolveCurrentUserId();

        Auction auction = adminService.approveAndCreateAuction(
                itemId,
                req.getStartTime(),
                req.getEndTime(),
                adminId,
                req.getStartingPrice() == null ? 0.0 : req.getStartingPrice(),
                req.getReservePrice()
        );

        return ResponseEntity.ok(toAuctionDto(auction));
    }

    @GetMapping("/items/pending")
    public ResponseEntity<List<PendingItemDto>> listPendingItems() {
        return ResponseEntity.ok(adminService.listPendingItems());
    }

    @PostMapping("/items/{itemId}/reject")
    public ResponseEntity<Item> rejectItem(@PathVariable UUID itemId) {
        UUID adminId = resolveCurrentUserId();
        return ResponseEntity.ok(adminService.rejectItem(itemId, adminId));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID itemId) {
        adminService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/reported")
    public ResponseEntity<List<PendingItemDto>> listReportedItems() {
        return ResponseEntity.ok(adminService.listReportedItems());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/wallet-activity/recent")
    public ResponseEntity<List<AdminWalletActivityDto>> getRecentWalletActivity(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(adminService.getRecentWalletActivity(limit));
    }

    @GetMapping("/notifications/recent")
    public ResponseEntity<List<AdminNotificationDto>> getRecentNotifications(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(adminService.getRecentNotifications(limit));
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Authenticated admin is required");
        }

        User user = userService.findByUsername(auth.getName());

        if (user == null) {
            throw new BusinessRuleException("Authenticated admin not found: " + auth.getName());
        }

        return user.getId();
    }

    private AuctionDto toAuctionDto(Auction a) {
        AuctionDto d = new AuctionDto();
        d.id = a.getId();
        d.itemId = a.getItemId();
        d.itemName = a.getTitle();
        d.title = a.getTitle();
        d.description = a.getDescription();
        d.imageUrl = AuctionImageResolver.resolvePrimaryImage(a);
        d.category = a.getCategory();
        d.sellerId = a.getSellerId();
        d.currentPrice = a.getCurrentPrice();
        d.bidCount = a.getBidCount() == null ? 0 : a.getBidCount();
        d.minNextBid = a.getMinNextBid() == null ? a.getCurrentPrice() + 1.0 : a.getMinNextBid();
        d.leaderId = a.getLeaderId();
        d.sellerName = null;
        d.leaderName = null;
        d.startTime = a.getStartTime();
        d.endTime = a.getEndTime();
        d.state = a.getState() == null ? null : a.getState().name();
        return d;
    }
}
