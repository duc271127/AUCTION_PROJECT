package com.team.backend.controller;

import com.team.backend.dto.ItemCreateRequest;
import com.team.backend.dto.ItemDto;
import com.team.backend.dto.ItemResponse;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.service.ItemService;
import com.team.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seller/items")
@Validated
public class SellerItemController {

    private final ItemService itemService;
    private final UserService userService;

    public SellerItemController(ItemService itemService, UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> listSellerItems(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = resolveSellerId(sellerIdHeader, sellerIdParam, null);
        return ResponseEntity.ok(itemService.findBySellerId(sellerId));
    }

    @PostMapping
    public ResponseEntity<ItemDto> createItem(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemDto dto) {

        UUID sellerId = resolveSellerId(sellerIdHeader, null, dto.getSellerId());
        dto.setSellerId(sellerId);

        ItemDto created = itemService.createForSeller(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemDto dto) {

        UUID sellerId = resolveSellerId(sellerIdHeader, null, dto.getSellerId());
        dto.setId(id);
        dto.setSellerId(sellerId);

        ItemDto updated = itemService.updateForSeller(id, sellerId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = resolveSellerId(sellerIdHeader, sellerIdParam, null);
        itemService.deleteForSeller(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/v2")
    public ResponseEntity<List<ItemResponse>> listSellerItemsV2(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = resolveSellerId(sellerIdHeader, sellerIdParam, null);
        return ResponseEntity.ok(itemService.findResponsesBySellerId(sellerId));
    }

    @PostMapping("/v2")
    public ResponseEntity<ItemResponse> createItemV2(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemCreateRequest request) {

        UUID sellerId = resolveSellerId(sellerIdHeader, null, request.getSellerId());
        ItemResponse created = itemService.createForSeller(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/v2/{id}")
    public ResponseEntity<ItemResponse> updateItemV2(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestBody @Valid ItemCreateRequest request) {

        UUID sellerId = resolveSellerId(sellerIdHeader, null, request.getSellerId());
        ItemResponse updated = itemService.updateForSeller(id, sellerId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/v2/{id}")
    public ResponseEntity<Void> deleteItemV2(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = resolveSellerId(sellerIdHeader, sellerIdParam, null);
        itemService.deleteForSellerResponse(id, sellerId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveSellerId(UUID sellerIdHeader, UUID sellerIdParam, UUID sellerIdBody) {
        UUID authenticatedSellerId = resolveAuthenticatedUserIdOrNull();
        if (authenticatedSellerId != null) {
            return authenticatedSellerId;
        }

        UUID fallbackSellerId = sellerIdHeader != null ? sellerIdHeader : sellerIdParam;
        if (fallbackSellerId == null) {
            fallbackSellerId = sellerIdBody;
        }

        if (fallbackSellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }

        return fallbackSellerId;
    }

    private UUID resolveAuthenticatedUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        User user = userService.findByUsername(auth.getName());
        return user == null ? null : user.getId();
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<String> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}