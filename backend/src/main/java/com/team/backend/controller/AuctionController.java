package com.team.backend.controller;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.AutoBidRequestDto;
import com.team.backend.dto.BidRequestDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AutoBid;
import com.team.backend.entity.BidTransaction;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.realtime.RealtimeEventType;
import com.team.backend.realtime.RealtimeNotifier;
import com.team.backend.service.AuctionService;
import com.team.backend.service.AutoBidService;
import com.team.backend.service.BidService;
import com.team.backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuctionController - controller đầy đủ cho các thao tác liên quan auction:
 * - create auction (seller/admin)
 * - place bid (authenticated)
 * - set auto-bid (authenticated)
 * - list auctions, get auction detail
 * - close auction (authenticated)
 *
 * Controller phát realtime events qua RealtimeNotifier sau khi thay đổi trạng thái.
 */
@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

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

    // -------------------------
    // Create auction endpoints
    // -------------------------

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AuctionDto> createAuction(@Valid @RequestBody AuctionCreateDto dto) {
        UUID sellerId = resolveUserIdFromSecurity();
        Auction created = auctionService.createAuction(dto, sellerId);
        URI location = URI.create("/api/auctions/" + created.getId());
        log.info("Auction created by seller {}: auctionId={}", sellerId, created.getId());
        return ResponseEntity.created(location).body(toDto(created));
    }

    @PostMapping("/seller/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuctionDto> createAuctionWithSeller(@PathVariable UUID sellerId,
                                                              @Valid @RequestBody AuctionCreateDto dto) {
        Auction created = auctionService.createAuction(dto, sellerId);
        URI location = URI.create("/api/auctions/" + created.getId());
        log.info("Auction created by admin {} for seller {}: auctionId={}", resolveUserIdFromSecurity(), sellerId, created.getId());
        return ResponseEntity.created(location).body(toDto(created));
    }

    // -------------------------
    // Place bid
    // -------------------------

    /**
     * Place a bid on an auction.
     * - bidderId can be provided via X-User-Id header (for testing) or resolved from security context.
     * - request body contains amount (and optional bidderId for non-authenticated test flows).
     * - returns updated AuctionDto.
     */
    @PostMapping("/{id}/bids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuctionDto> placeBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody BidRequestDto dto
    ) {

        UUID bidderId = userId != null ? userId : dto.bidderId;
        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        // read-before to capture old endTime for anti-sniping detection
        Auction beforeBid = auctionService.getAuction(id);
        Instant oldEndTime = beforeBid == null ? null : beforeBid.getEndTime();

        // place bid (service handles validation, locking, auto-bid, anti-sniping)
        bidService.placeBid(id, bidderId, dto.amount);

        // fetch updated auction and broadcast realtime events
        Auction updated = auctionService.getAuction(id);

        // broadcast bid placed event
        RealtimeEvent bidEvent = RealtimeEventFactory.bidPlaced(
                id,
                bidderId,
                dto.amount
        );

        // if auction was extended due to anti-sniping, broadcast extension event
        if (oldEndTime != null && updated.getEndTime() != null && updated.getEndTime().isAfter(oldEndTime)) {
            RealtimeEvent extendedEvent = RealtimeEventFactory.auctionExtended(
                    id,
                    extendedEvent
            );
        }

        return ResponseEntity.ok(
                toDto(updated)
        );
    }

    // -------------------------
    // Auto-bid endpoints
    // -------------------------

    @PostMapping("/{id}/auto-bid")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AutoBid> setAutoBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody AutoBidRequestDto dto
    ) {

        UUID bidderId = userId != null ? userId : dto.bidderId;
        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        AutoBid autoBid = autoBidService.setAutoBid(id, bidderId, dto.maxAmount);
        log.info("AutoBid set: auction={}, bidder={}, maxAmount={}", id, bidderId, dto.maxAmount);
        return ResponseEntity.ok(autoBid);
    }

    // -------------------------
    // Read endpoints
    // -------------------------

    @GetMapping
    public ResponseEntity<List<AuctionDto>> listAuctions() {

        List<AuctionDto> result =
                auctionService.listAuctions()
                        .stream()
                        .map(this::toDto)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

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

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidTransaction>> getBidHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getBidHistory(id));
    }

    // -------------------------
    // Admin / control endpoints
    // -------------------------

    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> closeAuction(@PathVariable UUID id) {
        auctionService.closeAuction(id);
        // broadcast auction finished event
        RealtimeEvent finished = RealtimeEventFactory.auctionFinished(id);
        realtimeNotifier.broadcastToAuction(id, finished);
        return ResponseEntity.noContent().build();
    }

    // -------------------------
    // Helpers
    // -------------------------

    private AuctionDto toDto(Auction a) {

        if (a == null) {
            return null;
        }

        AuctionDto d = new AuctionDto();

        d.id = a.getId();

        if (a.getItem() != null) {

            d.itemId =
                    a.getItem().getId();

            d.itemName =
                    a.getItem().getName();
        }
        d.currentPrice = a.getCurrentPrice();
        d.leaderId = a.getLeaderId();
        d.startTime = a.getStartTime();
        d.endTime = a.getEndTime();
        d.state = a.getState() == null ? null : a.getState().name();
        return d;
    }

        d.startTime =
                a.getStartTime();

        d.endTime =
                a.getEndTime();

        d.state =
                a.getState() == null
                        ? null
                        : a.getState().name();

        return d;
    }
}
