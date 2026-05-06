package com.team.backend.controller;

import com.team.backend.dto.ItemCreateRequest;
import com.team.backend.dto.ItemDto;
import com.team.backend.dto.ItemResponse;
import com.team.backend.entity.Item;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * SellerItemController - mở rộng:
 * - Legacy endpoints (ItemDto) để tương thích code cũ / internal API
 * - New endpoints (v2) trả ItemResponse / nhận ItemCreateRequest cho frontend
 *
 * Legacy endpoints:
 *   GET  /seller/items
 *   POST /seller/items
 *   PUT  /seller/items/{id}
 *   DELETE /seller/items/{id}
 *
 * Frontend endpoints (v2):
 *   GET  /seller/items/v2
 *   POST /seller/items/v2
 *   PUT  /seller/items/v2/{id}
 *   DELETE /seller/items/v2/{id}
 *
 * SellerId precedence:
 *   1) Header "X-Seller-Id"
 *   2) Query param "sellerId"
 *   3) Body field (for legacy ItemDto create/update)
 */
@RestController
@RequestMapping("/seller/items")
@Validated
public class SellerItemController {

    private final ItemService itemService;

    public SellerItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // -----------------------
    // Legacy endpoints (ItemDto)
    // -----------------------

    // GET /seller/items?sellerId={sellerId}
    @GetMapping
    public ResponseEntity<List<ItemDto>> listSellerItems(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : sellerIdParam;
        if (sellerId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ItemDto> items = itemService.findBySellerId(sellerId);
        return ResponseEntity.ok(items);
    }

    // POST /seller/items  (legacy create using ItemDto)
    @PostMapping
    public ResponseEntity<ItemDto> createItem(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemDto dto) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : dto.getSellerId();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        dto.setSellerId(sellerId);
        ItemDto created = itemService.createForSeller(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /seller/items/{id} (legacy update using ItemDto)
    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemDto dto) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : dto.getSellerId();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        dto.setId(id);
        dto.setSellerId(sellerId);
        ItemDto updated = itemService.updateForSeller(id, sellerId, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE /seller/items/{id} (legacy)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : sellerIdParam;
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        itemService.deleteForSeller(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------
    // New frontend endpoints (v2) using ItemCreateRequest / ItemResponse
    // -----------------------

    // GET /seller/items/v2?sellerId={sellerId}
    @GetMapping("/v2")
    public ResponseEntity<List<ItemResponse>> listSellerItemsV2(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : sellerIdParam;
        if (sellerId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ItemResponse> items = itemService.findResponsesBySellerId(sellerId);
        return ResponseEntity.ok(items);
    }

    // POST /seller/items/v2  (create for frontend)
    @PostMapping("/v2")
    public ResponseEntity<ItemResponse> createItemV2(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemCreateRequest request) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : request.getSellerId();

        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ItemResponse created = itemService.createForSeller(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /seller/items/v2/{id} (update for frontend)
    @PutMapping("/v2/{id}")
    public ResponseEntity<ItemResponse> updateItemV2(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemCreateRequest request) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : request.getSellerId();

        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ItemResponse updated = itemService.updateForSeller(id, sellerId, request);
        return ResponseEntity.ok(updated);
    }

    // DELETE /seller/items/v2/{id} (delete for frontend)
    @DeleteMapping("/v2/{id}")
    public ResponseEntity<Void> deleteItemV2(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : sellerIdParam;
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        itemService.deleteForSellerResponse(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------
    // Exception handlers
    // -----------------------
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<String> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // Generic fallback (optional)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleOther(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
