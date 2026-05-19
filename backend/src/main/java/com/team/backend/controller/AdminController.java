package com.team.backend.controller;

import com.team.backend.dto.CreateAuctionRequest;
import com.team.backend.dto.PendingItemDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.Item;
import com.team.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/items/{itemId}/approve")
    public ResponseEntity<Item> approveItem(
            @PathVariable UUID itemId,
            @RequestHeader(value = "X-Admin-Id", required = false) UUID adminId) {

        Item approved = adminService.approveItem(itemId, adminId);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/items/{itemId}/create-auction")
    public ResponseEntity<Auction> createAuction(
            @PathVariable UUID itemId,
            @RequestBody CreateAuctionRequest req,
            @RequestHeader(value = "X-Admin-Id", required = false) UUID adminId) {

        Auction auction = adminService.createAuctionForItem(
                itemId,
                req.getStartTime(),
                req.getEndTime(),
                adminId,
                req.getStartingPrice(),
                req.getReservePrice()
        );

        return ResponseEntity.ok(auction);
    }

    @GetMapping("/items/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingItemDto>> listPendingItems() {
        return ResponseEntity.ok(adminService.listPendingItems());
    }
}
