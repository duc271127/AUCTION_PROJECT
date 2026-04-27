package com.team.backend.controller;

import com.team.backend.dto.ItemDto;
import com.team.backend.entity.Item;
import com.team.backend.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller tương thích với frontend SellerItemApiService:
 * - GET  /seller/items
 * - POST /seller/items
 * - PUT  /seller/items/{id}
 * - DELETE /seller/items/{id}
 *
 * Lưu ý: sellerId có thể truyền qua header, query param hoặc trong body.
 * Ở đây mình hỗ trợ 2 cách: nếu sellerId có trong header "X-Seller-Id" thì ưu tiên,
 * nếu không có thì lấy từ body (ItemDto.sellerId).
 */
@RestController
@RequestMapping("/seller/items")
@Validated
public class SellerItemController {

    private final ItemService itemService;

    public SellerItemController(ItemService itemService) {
        this.itemService = itemService;
    }

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

    // POST /seller/items
    @PostMapping
    public ResponseEntity<ItemDto> createItem(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Validated ItemDto dto) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : dto.getSellerId();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        dto.setSellerId(sellerId);
        ItemDto created = itemService.createForSeller(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /seller/items/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Validated ItemDto dto) {

        UUID sellerId = sellerIdHeader != null ? sellerIdHeader : dto.getSellerId();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        dto.setId(id);
        dto.setSellerId(sellerId);
        ItemDto updated = itemService.updateForSeller(id, sellerId, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE /seller/items/{id}
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
}
