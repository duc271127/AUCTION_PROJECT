package com.team.backend.controller;

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

<<<<<<< Updated upstream
//    @PostMapping
//    public ResponseEntity<BidTransaction> placeBid(
//            @PathVariable UUID auctionId,
//            @RequestHeader("X-User-Id") UUID userId,
//            @RequestBody PlaceBidRequest req
//    ) {
//
//        BidTransaction b =
//                bidService.placeBid(
//                        auctionId,
//                        userId,
//                        req.amount
//                );
//
//        return ResponseEntity.ok(b);
//    }
//
//    public static class PlaceBidRequest {
//
//        public double amount;
//
//    }
=======
    @PostMapping
    public ResponseEntity<BidTransaction> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PlaceBidRequest req
    ) {
        if (userId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) là bắt buộc");
        }
        if (req == null || req.amount <= 0.0) {
            throw new BusinessRuleException("Số tiền phải lớn hơn 0");
        }

        log.debug("Yêu cầu đặt giá: auctionId={}, userId={}, amount={}", auctionId, userId, req.amount);

        BidTransaction b = bidService.placeBid(
                auctionId,
                userId,
                req.amount
        );

        return ResponseEntity.ok(b);
    }

    @GetMapping("/history")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(
            @PathVariable UUID auctionId,
            @RequestParam(value = "limit", required = false) @Min(1) Integer limit
    ) {
        List<BidHistoryDto> history;
        if (limit == null) {
            history = bidService.getBidHistory(auctionId);
        } else {
            history = bidService.getBidHistory(auctionId);
        }
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

    // Helper nội bộ để tái sử dụng logic đặt giá (validate + logging)
    private BidTransaction placeBidInternal(UUID auctionId, UUID userId, PlaceBidRequest req) {
        if (userId == null) {
            throw new BusinessRuleException("Header X-User-Id (bidderId) là bắt buộc");
        }
        if (req == null || req.amount <= 0.0) {
            throw new BusinessRuleException("Số tiền phải lớn hơn 0");
        }
        return bidService.placeBid(auctionId, userId, req.amount);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("endpoint đặt giá hoạt động");
    }

    public static class PlaceBidRequest {

        @Min(value = 0, message = "Số tiền phải không âm")
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
>>>>>>> Stashed changes
}
