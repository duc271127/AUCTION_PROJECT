package com.team.backend.controller;

import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.BidTransaction;
import com.team.backend.service.BidService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public ResponseEntity<BidTransaction> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody PlaceBidRequest req
    ) {

        BidTransaction b =
                bidService.placeBid(
                        auctionId,
                        userId,
                        req.amount
                );

        return ResponseEntity.ok(b);
    }

    @GetMapping("/history")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(
            @PathVariable UUID auctionId
    ) {

        return ResponseEntity.ok(
                bidService.getBidHistory(auctionId)
        );
    }

    public static class PlaceBidRequest {

        public double amount;

    }
}