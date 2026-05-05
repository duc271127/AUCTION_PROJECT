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

    public AdminController(AdminService adminService) { this.adminService = adminService; }

    // Admin only
    @PostMapping("/items/{itemId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<Item> approveItem(@PathVariable UUID itemId,
                                            @RequestHeader(value = "X-Admin-Id", required = false) UUID adminId) {
        // In production, adminId should be taken from security context (JWT), header used for dev/testing
        UUID actor = adminId;
        Item approved = adminService.approveItem(itemId, actor);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/items/{itemId}/create-auction")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<Auction> createAuction(@PathVariable UUID itemId,
                                                 @RequestBody CreateAuctionRequest req,
                                                 @RequestHeader(value = "X-Admin-Id", required = false) UUID adminId) {
        UUID actor = adminId;
        Auction a = adminService.createAuctionForItem(itemId, req.getStartTime(), req.getEndTime(), actor, req.getStartingPrice(), req.getReservePrice());
        return ResponseEntity.ok(a);
    }

    @GetMapping("/items/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingItemDto>> listPendingItems() {
        List<PendingItemDto> dtos = adminService.listPendingItems();
        return ResponseEntity.ok(dtos);
    }
}
