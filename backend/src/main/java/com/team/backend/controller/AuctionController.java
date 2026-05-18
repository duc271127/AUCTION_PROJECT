package com.team.backend.controller;

import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.BidRequestDto;
import com.team.backend.entity.BidTransaction;
import com.team.backend.entity.Auction;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.service.AuctionService;
import com.team.backend.service.BidService;
import com.team.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

/**
 * AuctionController - phiên bản tương thích với AuctionService:
 * - listAuctions() không nhận tham số
 * - closeAuction(UUID) chỉ nhận 1 tham số
 */
@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final UserService userService;
    private final RealtimeNotifier realtimeNotifier;

    public AuctionController(AuctionService auctionService,
                             BidService bidService,
                             UserService userService,RealtimeNotifier realtimeNotifier) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.userService = userService;
        this.realtimeNotifier =realtimeNotifier;
    }

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

    /**
     * Dev helper: create auction with explicit sellerId in path.
     * Protected: only ADMIN can use this endpoint.
     */
    @PostMapping("/seller/{sellerId}")
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

        bidService.placeBid(id, bidderId, dto.amount);

        Auction updated = auctionService.getAuction(id);
        RealtimeEvent event = RealtimeEventFactory.bidPlaced(
                id,
                bidderId,
                updated.getCurrentPrice(),
                updated.getEndTime()
        );

        realtimeNotifier.broadcastToAuction(id, event);
        return ResponseEntity.ok(toDto(updated));
    }

    /**
     * List all auctions (service signature: listAuctions()).
     */
    @GetMapping
    public ResponseEntity<List<AuctionDto>> listAuctions() {
        List<Auction> auctions = auctionService.listAuctions();
        List<AuctionDto> dtos = auctions.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get auction detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionDto> getAuction(@PathVariable UUID id) {
        Auction a = auctionService.getAuction(id);
        return ResponseEntity.ok(toDto(a));
    }

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

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidTransaction>> getBidHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getBidHistory(id));
    }

    // -------------------------
    // Helpers
    // -------------------------
    private AuctionDto toDto(Auction a) {
        if (a == null) return null;

        AuctionDto d = new AuctionDto();
        d.id = a.getId();
        if (a.getItem() != null) {
            d.itemId = a.getItem().getId();
            d.itemName = a.getItem().getName();
        }

        // Auction.getCurrentPrice() is primitive double in this project
        d.currentPrice = a.getCurrentPrice();

        d.leaderId = a.getLeaderId();
        d.startTime = a.getStartTime();
        d.endTime = a.getEndTime();
        d.state = a.getState() == null ? null : a.getState().name();
        return d;
    }

    private UUID resolveUserIdFromSecurity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessRuleException("Unauthenticated: user cannot be resolved");
        }
        String username = auth.getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Authenticated user not found: " + username);
        }
        return user.getId();
    }
}
