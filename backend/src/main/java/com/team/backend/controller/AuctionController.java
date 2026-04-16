package com.team.backend.controller;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.BidRequestDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.BidTransaction;
import com.team.backend.entity.User;
import com.team.backend.service.AuctionService;
import com.team.backend.service.BidService;
import com.team.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuctionController (fixed)
 */
@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final UserService userService;

    public AuctionController(AuctionService auctionService,
                             BidService bidService,
                             UserService userService) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.userService = userService;
    }

    /**
     * Create auction using authenticated user as seller.
     */
    @PostMapping
    public ResponseEntity<AuctionDto> createAuction(@Valid @RequestBody AuctionCreateDto dto) {
        UUID sellerId = resolveSellerIdFromSecurity();
        Auction created = auctionService.createAuction(dto, sellerId);
        return ResponseEntity.ok(toDto(created));
    }

    /**
     * Dev helper: create auction with explicit sellerId in path.
     */
    @PostMapping("/seller/{sellerId}")
    public ResponseEntity<AuctionDto> createAuctionWithSeller(@PathVariable("sellerId") UUID sellerId,
                                                              @Valid @RequestBody AuctionCreateDto dto) {
        Auction created = auctionService.createAuction(dto, sellerId);
        return ResponseEntity.ok(toDto(created));
    }

    /**
     * Place a bid on auction.
     */
    @PostMapping("/{id}/bids")
    public ResponseEntity<AuctionDto> placeBid(@PathVariable("id") UUID auctionId,
                                               @Valid @RequestBody BidRequestDto dto) {
        // place bid (service will validate)
        bidService.placeBid(auctionId, dto.bidderId, dto.amount);
        Auction updated = auctionService.getAuction(auctionId);
        return ResponseEntity.ok(toDto(updated));
    }

    @GetMapping
    public ResponseEntity<List<AuctionDto>> listAuctions() {
        List<Auction> auctions = auctionService.listAuctions();
        List<AuctionDto> dtos = auctions.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDto> getAuction(@PathVariable("id") UUID id) {
        Auction a = auctionService.getAuction(id);
        return ResponseEntity.ok(toDto(a));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeAuction(@PathVariable("id") UUID id) {
        auctionService.closeAuction(id);
        return ResponseEntity.ok().build();
    }

    // -------------------------
    // Helpers
    // -------------------------
    private AuctionDto toDto(Auction a) {
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
        d.state = a.getState() == null ? null : a.getState().name();
        return d;
    }

    private UUID resolveSellerIdFromSecurity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new com.team.backend.exception.BusinessRuleException("Unauthenticated: sellerId cannot be resolved");
        }
        String username = auth.getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new com.team.backend.exception.ResourceNotFoundException("Authenticated user not found: " + username);
        }
        return user.getId();
    }
}
