package com.team.backend.controller;

import com.team.backend.dto.AutoBidRequestDto;
import com.team.backend.entity.AutoBid;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.service.AutoBidService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions/{auctionId}/auto-bid")
@Validated
public class AutoBidController {

    private static final Logger log = LoggerFactory.getLogger(AutoBidController.class);

    private final AutoBidService autoBidService;

    public AutoBidController(AutoBidService autoBidService) {
        this.autoBidService = autoBidService;
    }

    @PostMapping
    public ResponseEntity<AutoBid> setAutoBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID bidderId,
            @Valid @RequestBody AutoBidRequestDto dto
    ) {
        if (bidderId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) là bắt buộc");
        }
        if (dto == null || dto.getMaxAmount() <= 0.0) {
            throw new BusinessRuleException("maxAmount phải lớn hơn 0");
        }

        log.debug("Yêu cầu setAutoBid: auctionId={}, bidderId={}, maxAmount={}", auctionId, bidderId, dto.getMaxAmount());

        AutoBid saved = autoBidService.setAutoBid(auctionId, bidderId, dto.getMaxAmount());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelAutoBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID bidderId
    ) {
        if (bidderId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) là bắt buộc");
        }

        log.debug("Yêu cầu cancelAutoBid: auctionId={}, bidderId={}", auctionId, bidderId);

        autoBidService.cancelAutoBid(auctionId, bidderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AutoBid>> listAutoBidsForAuction(
            @PathVariable UUID auctionId
    ) {
        List<AutoBid> list = autoBidService.listAutoBidsForAuction(auctionId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AutoBid>> listMyAutoBids(
            @RequestHeader("X-User-Id") UUID bidderId
    ) {
        if (bidderId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) là bắt buộc");
        }
        List<AutoBid> list = autoBidService.listAutoBidsByUser(bidderId);
        return ResponseEntity.ok(list);
    }

    @PatchMapping
    public ResponseEntity<AutoBid> updateAutoBidMax(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID bidderId,
            @RequestParam("maxAmount") @Min(value = 1, message = "maxAmount phải >= 1") double maxAmount
    ) {
        if (bidderId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) là bắt buộc");
        }
        if (maxAmount <= 0) {
            throw new BusinessRuleException("maxAmount phải lớn hơn 0");
        }

        log.debug("Yêu cầu updateAutoBidMax: auctionId={}, bidderId={}, maxAmount={}", auctionId, bidderId, maxAmount);

        AutoBid updated = autoBidService.setAutoBid(auctionId, bidderId, maxAmount);
        return ResponseEntity.ok(updated);
    }

    public static class SimpleAutoBidRequest {
        @Min(value = 1, message = "maxAmount phải >= 1")
        public double maxAmount;

        public SimpleAutoBidRequest() {}

        public SimpleAutoBidRequest(double maxAmount) {
            this.maxAmount = maxAmount;
        }

        public double getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(double maxAmount) {
            this.maxAmount = maxAmount;
        }
    }
}
