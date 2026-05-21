package com.team.backend.controller;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.AutoBidRequestDto;
import com.team.backend.dto.BidHistoryDto;
import com.team.backend.dto.BidRequestDto;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AutoBid;

import com.team.backend.exception.BusinessRuleException;

import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.realtime.RealtimeNotifier;

import com.team.backend.service.AuctionService;
import com.team.backend.service.AutoBidService;
import com.team.backend.service.BidService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final AutoBidService autoBidService;
    private final RealtimeNotifier realtimeNotifier;

    public AuctionController(
            AuctionService auctionService,
            BidService bidService,
            AutoBidService autoBidService,
            RealtimeNotifier realtimeNotifier
    ) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.realtimeNotifier = realtimeNotifier;
    }

    // =========================
    // CREATE AUCTION
    // =========================

    @PostMapping("/seller/{sellerId}")
    public ResponseEntity<AuctionDto> createAuction(
            @PathVariable UUID sellerId,
            @Valid @RequestBody AuctionCreateDto dto
    ) {

        Auction created =
                auctionService.createAuction(dto, sellerId);

        URI location =
                URI.create("/api/auctions/" + created.getId());

        return ResponseEntity
                .created(location)
                .body(toDto(created));
    }

    // =========================
    // PLACE BID
    // =========================

    @PostMapping("/{id}/bids")
    public ResponseEntity<AuctionDto> placeBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody BidRequestDto dto
    ) {

        UUID bidderId =
                userId != null ? userId : dto.bidderId;

        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        Auction beforeBid =
                auctionService.getAuction(id);

        var oldEndTime =
                beforeBid.getEndTime();

        bidService.placeBid(
                id,
                bidderId,
                dto.amount
        );

        Auction updated =
                auctionService.getAuction(id);

        RealtimeEvent bidEvent =
                RealtimeEventFactory.bidPlaced(
                        id,
                        bidderId,
                        updated.getCurrentPrice(),
                        updated.getEndTime()
                );

        realtimeNotifier.broadcastToAuction(
                id,
                bidEvent
        );

        if (oldEndTime != null
                && updated.getEndTime() != null
                && updated.getEndTime().isAfter(oldEndTime)) {

            RealtimeEvent extendedEvent =
                    RealtimeEventFactory.auctionExtended(
                            id,
                            bidderId,
                            updated.getCurrentPrice(),
                            updated.getEndTime()
                    );

            realtimeNotifier.broadcastToAuction(
                    id,
                    extendedEvent
            );
        }

        return ResponseEntity.ok(
                toDto(updated)
        );
    }

    // =========================
    // AUTO BID
    // =========================

    @PostMapping("/{id}/auto-bid")
    public ResponseEntity<AutoBid> setAutoBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody AutoBidRequestDto dto
    ) {

        UUID bidderId = userId;

        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        AutoBid autoBid =
                autoBidService.setAutoBid(
                        id,
                        bidderId,
                        dto.getMaxAmount()
                );

        return ResponseEntity.ok(autoBid);
    }

    @DeleteMapping("/{id}/auto-bid")
    public ResponseEntity<Void> cancelAutoBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        UUID bidderId = userId;
        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        autoBidService.cancelAutoBid(id, bidderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/auto-bids")
    public ResponseEntity<List<AutoBid>> listAutoBidsForAuction(
            @PathVariable UUID id
    ) {
        List<AutoBid> list = autoBidService.listAutoBidsForAuction(id);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me/auto-bids")
    public ResponseEntity<List<AutoBid>> listMyAutoBids(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        if (userId == null) {
            throw new BusinessRuleException("bidderId is required");
        }
        List<AutoBid> list = autoBidService.listAutoBidsByUser(userId);
        return ResponseEntity.ok(list);
    }

    // =========================
    // GET ALL AUCTIONS
    // =========================

    @GetMapping
    public ResponseEntity<List<AuctionDto>> listAuctions() {

        List<AuctionDto> result =
                auctionService.listAuctions()
                        .stream()
                        .map(this::toDto)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // =========================
    // GET AUCTION
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDto> getAuction(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toDto(
                        auctionService.getAuction(id)
                )
        );
    }

    // =========================
    // BID HISTORY
    // =========================

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(
            @PathVariable UUID id,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        if (limit == null) {
            return ResponseEntity.ok(
                    bidService.getBidHistory(id)
            );
        } else {
            return ResponseEntity.ok(
                    bidService.getBidHistory(id)
            );
        }
    }

    // =========================
    // AUCTION SUMMARY / METADATA
    // =========================

    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getAuctionSummary(
            @PathVariable UUID id
    ) {
        Map<String, Object> summary = bidService.getAuctionSummary(id);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/leader")
    public ResponseEntity<UUID> getCurrentLeader(
            @PathVariable UUID id
    ) {
        UUID leader = bidService.getCurrentLeader(id);
        return ResponseEntity.ok(leader);
    }

    @GetMapping("/config/min-increment")
    public ResponseEntity<Double> getMinIncrement() {
        double min = bidService.getMinIncrement();
        return ResponseEntity.ok(min);
    }

    // =========================
    // CLOSE AUCTION
    // =========================

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeAuction(
            @PathVariable UUID id
    ) {

        auctionService.closeAuction(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // DTO MAPPER
    // =========================

    private AuctionDto toDto(Auction a) {

        if (a == null) {
            return null;
        }

        AuctionDto d = new AuctionDto();

        d.id = a.getId();

        if (a.getItem() != null) {

            d.itemId = a.getItem().getId();
            d.itemName = a.getItem().getName();
        }

        d.currentPrice = a.getCurrentPrice();
        d.leaderId = a.getLeaderId();
        d.startTime = a.getStartTime();
        d.endTime = a.getEndTime();

        d.state =
                a.getState() == null
                        ? null
                        : a.getState().name();

        return d;
    }
}
