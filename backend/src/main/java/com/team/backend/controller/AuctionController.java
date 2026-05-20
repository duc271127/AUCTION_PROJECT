package com.team.backend.controller;

import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
<<<<<<< Updated upstream
import com.team.backend.dto.BidRequestDto;
import com.team.backend.dto.AutoBidRequestDto;
import com.team.backend.entity.AutoBid;
import com.team.backend.service.AutoBidService;
import com.team.backend.entity.BidTransaction;
import com.team.backend.entity.Auction;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.service.AuctionService;
import com.team.backend.service.BidService;
import com.team.backend.service.UserService;
=======
import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.Auction;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.realtime.RealtimeNotifier;
import com.team.backend.service.AuctionService;
import com.team.backend.service.BidService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> Stashed changes
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventType;
import com.team.backend.realtime.RealtimeNotifier;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

<<<<<<< Updated upstream
/**
 * AuctionController - phiên bản tương thích với AuctionService:
 * - listAuctions() không nhận tham số
 * - closeAuction(UUID) chỉ nhận 1 tham số
 */
=======
>>>>>>> Stashed changes
@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

<<<<<<< Updated upstream
=======
    private static final Logger log =
            LoggerFactory.getLogger(AuctionController.class);

>>>>>>> Stashed changes
    private final AuctionService auctionService;
    private final BidService bidService;
    private final RealtimeNotifier realtimeNotifier;

    public AuctionController(
            AuctionService auctionService,
            BidService bidService,
            RealtimeNotifier realtimeNotifier
    ) {

        this.auctionService = auctionService;
        this.bidService = bidService;
        this.realtimeNotifier = realtimeNotifier;
    }

<<<<<<< Updated upstream
    /**
     * Create auction using authenticated user as seller.
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AuctionDto> createAuction(@Valid @RequestBody AuctionCreateDto dto) {
        UUID sellerId = resolveUserIdFromSecurity();
        Auction created = auctionService.createAuction(dto, sellerId);
        URI location = URI.create("/api/auctions/" + created.getId());
        return ResponseEntity.created(location).body(toDto(created));
    }
=======
    // =========================
    // CREATE AUCTION
    // =========================
>>>>>>> Stashed changes

    /**
     * Dev helper: create auction with explicit sellerId in path.
     * Protected: only ADMIN can use this endpoint.
     */
    @PostMapping("/seller/{sellerId}")
<<<<<<< Updated upstream
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuctionDto> createAuctionWithSeller(@PathVariable UUID sellerId,
                                                              @Valid @RequestBody AuctionCreateDto dto) {
        Auction created = auctionService.createAuction(dto, sellerId);
        URI location = URI.create("/api/auctions/" + created.getId());
        return ResponseEntity.created(location).body(toDto(created));
    }

    /**
     * Place a bid on auction.
     * Bidder is resolved from authenticated user; request must contain only amount.
     */
    /*
    @PostMapping("/{id}/bids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuctionDto> placeBid(@PathVariable UUID id,
                                               @Valid @RequestBody BidRequestDto dto) {
        UUID bidderId = resolveUserIdFromSecurity();
        bidService.placeBid(id, bidderId, dto.amount);
        Auction updated = auctionService.getAuction(id);
        return ResponseEntity.ok(toDto(updated));
    }

     */
    @PostMapping("/{id}/bids")
    public ResponseEntity<AuctionDto> placeBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody BidRequestDto dto) {

        UUID bidderId = userId != null ? userId : dto.bidderId;

        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        Auction beforeBid = auctionService.getAuction(id);
        var oldEndTime = beforeBid.getEndTime();

        bidService.placeBid(id, bidderId, dto.amount);

        Auction updated = auctionService.getAuction(id);

        RealtimeEvent bidEvent = RealtimeEventFactory.bidPlaced(
                id,
                bidderId,
                updated.getCurrentPrice(),
                updated.getEndTime()
=======
    public ResponseEntity<AuctionDto> createAuction(
            @PathVariable UUID sellerId,
            @Valid @RequestBody AuctionCreateDto dto
    ) {

        Auction created =
                auctionService.createAuction(dto, sellerId);

        URI location =
                URI.create("/api/auctions/" + created.getId());

        log.info(
                "Auction created for seller {} : {}",
                sellerId,
                created.getId()
>>>>>>> Stashed changes
        );

<<<<<<< Updated upstream
        if (oldEndTime != null && updated.getEndTime() != null
                && updated.getEndTime().isAfter(oldEndTime)) {
            RealtimeEvent extendedEvent = RealtimeEventFactory.auctionExtended(
                    id,
                    bidderId,
                    updated.getCurrentPrice(),
                    updated.getEndTime()
            );
            realtimeNotifier.broadcastToAuction(id, extendedEvent);
        }

        return ResponseEntity.ok(toDto(updated));
    }
    @PostMapping("/{id}/auto-bid")
    public ResponseEntity<AutoBid> setAutoBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody AutoBidRequestDto dto) {

        UUID bidderId = userId != null ? userId : dto.bidderId;

        if (bidderId == null) {
            throw new BusinessRuleException("bidderId is required");
        }

        AutoBid autoBid = autoBidService.setAutoBid(id, bidderId, dto.maxAmount);
        return ResponseEntity.ok(autoBid);
    }

    /**
     * List all auctions (service signature: listAuctions()).
     */
=======
        return ResponseEntity
                .created(location)
                .body(toDto(created));
    }

    // =========================
    // GET ALL AUCTIONS
    // =========================

>>>>>>> Stashed changes
    @GetMapping
    public ResponseEntity<List<AuctionDto>> listAuctions() {

        List<AuctionDto> result =
                auctionService.listAuctions()
                        .stream()
                        .map(this::toDto)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

<<<<<<< Updated upstream
    /**
     * Get auction detail.
     */
=======
    // =========================
    // GET AUCTION
    // =========================

>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    /**
     * Close auction (force close).
     * Service handles authorization/audit if needed.
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> closeAuction(@PathVariable UUID id) {
        // call service with single-arg signature
        auctionService.closeAuction(id);
        return ResponseEntity.noContent().build();
    }
=======
    // =========================
    // BID HISTORY
    // =========================
>>>>>>> Stashed changes

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                bidService.getBidHistory(id)
        );
    }

<<<<<<< Updated upstream
    // -------------------------
    // Helpers
    // -------------------------
=======
    // =========================
    // CLOSE AUCTION
    // =========================

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeAuction(
            @PathVariable UUID id
    ) {

        auctionService.closeAuction(id);

        RealtimeEvent finished =
                RealtimeEventFactory.auctionFinished(id);

        realtimeNotifier.broadcastToAuction(
                id,
                finished
        );

        return ResponseEntity.noContent().build();
    }

    // =========================
    // DTO MAPPER
    // =========================

>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
        // Auction.getCurrentPrice() is primitive double in this project
        d.currentPrice = a.getCurrentPrice();

        d.leaderId = a.getLeaderId();
        d.startTime = a.getStartTime();
        d.endTime = a.getEndTime();
        d.state = a.getState() == null ? null : a.getState().name();
=======
        d.currentPrice =
                a.getCurrentPrice();

        d.leaderId =
                a.getLeaderId();

        d.startTime =
                a.getStartTime();

        d.endTime =
                a.getEndTime();

        d.state =
                a.getState() == null
                        ? null
                        : a.getState().name();

>>>>>>> Stashed changes
        return d;
    }
}