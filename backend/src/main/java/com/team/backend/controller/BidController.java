package com.team.backend.controller;

import com.team.backend.bidding.BidProcessingResult;
import com.team.backend.bidding.BidRequest;
import com.team.backend.concurrent.AuctionRegistry;
import com.team.backend.concurrent.AuctionState;
import com.team.backend.concurrent.ConcurrentBidProcessor;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeNotifier;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
public class BidController {

    private final AuctionRegistry auctionRegistry;
    private final ConcurrentBidProcessor concurrentBidProcessor;
    private final RealtimeNotifier realtimeNotifier;

    public BidController(AuctionRegistry auctionRegistry,
                         ConcurrentBidProcessor concurrentBidProcessor,
                         RealtimeNotifier realtimeNotifier) {
        this.auctionRegistry = auctionRegistry;
        this.concurrentBidProcessor = concurrentBidProcessor;
        this.realtimeNotifier = realtimeNotifier;
    }

    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<?> placeBid(@PathVariable Long auctionId,
                                      @Valid @RequestBody BidRequest request) {
        AuctionState auction = auctionRegistry.getAuction(auctionId);

        if (auction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Auction not found");
        }

        BidProcessingResult result = concurrentBidProcessor.processBid(
                auction,
                request.getBidderName(),
                request.getBidAmount()
        );

        if (result.isAccepted()) {
            for (RealtimeEvent event : result.getEvents()) {
                realtimeNotifier.broadcastToAuction(auctionId, event);
            }
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<?> getAuction(@PathVariable Long auctionId) {
        AuctionState auction = auctionRegistry.getAuction(auctionId);

        if (auction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Auction not found");
        }

        return ResponseEntity.ok(auction);
    }

    @PostMapping("/{auctionId}/close")
    public ResponseEntity<?> closeAuction(@PathVariable Long auctionId) {
        AuctionState auction = auctionRegistry.getAuction(auctionId);

        if (auction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Auction not found");
        }

        BidProcessingResult result = concurrentBidProcessor.closeAuction(auction);

        if (result.isAccepted()) {
            for (RealtimeEvent event : result.getEvents()) {
                realtimeNotifier.broadcastToAuction(auctionId, event);
            }
        }

        return ResponseEntity.ok(result);
    }
}