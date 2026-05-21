package com.team.backend.controller;

import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.BidTransaction;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.service.BidService;
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
@RequestMapping("/api/auctions/{auctionId}/bids")
@Validated
public class BidController {

    private static final Logger log = LoggerFactory.getLogger(BidController.class);

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public ResponseEntity<BidTransaction> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PlaceBidRequest req
    ) {
        BidTransaction bid = placeBidInternal(auctionId, userId, req);
        return ResponseEntity.ok(bid);
    }

    @GetMapping("/history")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(
            @PathVariable UUID auctionId,
            @RequestParam(value = "limit", required = false) @Min(1) Integer limit
    ) {
        List<BidHistoryDto> history = limit == null
                ? bidService.getBidHistory(auctionId)
                : bidService.getBidHistory(auctionId, limit);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/place")
    public ResponseEntity<UUID> placeBidSimple(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PlaceBidRequest req
    ) {
        BidTransaction tx = placeBidInternal(auctionId, userId, req);
        return ResponseEntity.ok(tx.getId());
    }

    private BidTransaction placeBidInternal(UUID auctionId, UUID userId, PlaceBidRequest req) {
        if (userId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) is required");
        }
        if (req == null || req.amount <= 0.0) {
            throw new BusinessRuleException("Bid amount must be greater than 0");
        }

        log.debug("Place bid request: auctionId={}, userId={}, amount={}", auctionId, userId, req.amount);
        return bidService.placeBid(auctionId, userId, req.amount);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("bid endpoint is active");
    }

    public static class PlaceBidRequest {

        @Min(value = 0, message = "Bid amount must be non-negative")
        public double amount;

        public PlaceBidRequest() {
        }

        public PlaceBidRequest(double amount) {
            this.amount = amount;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
}
