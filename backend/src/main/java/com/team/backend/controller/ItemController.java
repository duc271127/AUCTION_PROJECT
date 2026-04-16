package com.team.backend.controller;

import com.team.backend.dto.ItemCreateDto;
import com.team.backend.entity.Item;
import com.team.backend.dto.UserDto;
import com.team.backend.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;

/**
 * Item endpoints. For Phase 2 we assume the caller provides sellerId (or we derive from Principal).
 * In a real app, derive sellerId from authenticated user.
 */
@RestController
@RequestMapping("/api/items")
@Validated
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) { this.itemService = itemService; }

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody ItemCreateDto dto, Principal principal) {
        // For Phase 2: we accept sellerId via Principal name (username) mapping to userId.
        // Here we assume a helper method to resolve sellerId from principal; for now, require header "X-Seller-Id" as UUID.
        // Simpler: read sellerId from request header (dev mode).
        // In production, use authentication to get current user id.
        throw new UnsupportedOperationException("Use createItemWithSellerId or integrate authentication to resolve sellerId");
    }

    // Dev helper: create item with explicit sellerId in path (for Phase 2 quick testing)
    @PostMapping("/seller/{sellerId}")
    public ResponseEntity<Item> createItemWithSeller(@PathVariable("sellerId") UUID sellerId, @Valid @RequestBody ItemCreateDto dto) {
        Item created = itemService.createItem(dto, sellerId);
        return ResponseEntity.ok(created);
    }
}
