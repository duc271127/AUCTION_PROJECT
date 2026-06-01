package com.auction.server.controller;

import com.auction.server.dto.AuctionCreateDto;
import com.auction.server.dto.AuctionDetailResponse;
import com.auction.server.dto.AuctionDto;
import com.auction.server.dto.AuctionPageResponse;
import com.auction.server.dto.AuctionSummaryResponse;
import com.auction.server.dto.AutoBidRequestDto;
import com.auction.server.dto.AutoBidResponse;
import com.auction.server.dto.BidHistoryDto;
import com.auction.server.dto.BidPlacementResponse;
import com.auction.server.dto.BidRequestDto;
import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.AutoBid;
import com.auction.server.entity.BidTransaction;
import com.auction.server.entity.User;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.service.AuctionHelper;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;
import com.auction.server.util.AuctionImageResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@Validated
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final AutoBidService autoBidService;
    private final UserService userService;
    private final AuctionHelper auctionHelper;

    public AuctionController(AuctionService auctionService,
                             BidService bidService,
                             AutoBidService autoBidService,
                             UserService userService,
                             AuctionHelper auctionHelper) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.userService = userService;
        this.auctionHelper = auctionHelper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<AuctionDto> createAuction(@Valid @RequestBody AuctionCreateDto dto) {
        UUID sellerId = resolveCurrentUserId();
        Auction created = auctionService.createAuction(dto, sellerId);
        return ResponseEntity.created(URI.create("/api/auctions/" + created.getId())).body(toDto(created));
    }

    @PostMapping("/seller/{sellerId}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<AuctionDto> createAuctionForSeller(@PathVariable UUID sellerId,
                                                             @Valid @RequestBody AuctionCreateDto dto) {
        UUID currentUserId = resolveCurrentUserId();
        if (!currentUserId.equals(sellerId) && !currentUserHasRole("ADMIN")) {
            throw new AccessDeniedException("You cannot create auctions for another seller");
        }

        Auction created = auctionService.createAuction(dto, sellerId);
        return ResponseEntity.created(URI.create("/api/auctions/" + created.getId())).body(toDto(created));
    }

    @PostMapping("/{id}/bids")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<BidPlacementResponse> placeBid(@PathVariable UUID id,
                                                         @Valid @RequestBody BidRequestDto dto) {
        UUID bidderId = resolveCurrentUserId();
        BidTransaction bid = bidService.placeBid(id, bidderId, dto.amount);
        Auction updated = auctionService.getAuction(id);
        return ResponseEntity.ok(toBidPlacementResponse(bid, updated));
    }

    @PostMapping("/{id}/auto-bid")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<AutoBidResponse> setAutoBid(@PathVariable UUID id,
                                                      @Valid @RequestBody AutoBidRequestDto dto) {
        UUID bidderId = resolveCurrentUserId();
        AutoBid autoBid = autoBidService.setAutoBid(id, bidderId, dto.getMaxAmount(), dto.getBidStep());
        return ResponseEntity.ok(toAutoBidResponse(autoBid, auctionService.getAuction(id)));
    }

    @DeleteMapping("/{id}/auto-bid")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<Void> cancelAutoBid(@PathVariable UUID id) {
        autoBidService.cancelAutoBid(id, resolveCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/auto-bids")
    @PreAuthorize("hasAnyRole('BIDDER','ADMIN')")
    public ResponseEntity<List<AutoBidResponse>> listAutoBidsForAuction(@PathVariable UUID id) {
        Auction auction = auctionService.getAuction(id);
        return ResponseEntity.ok(autoBidService.listAutoBidsForAuction(id)
                .stream()
                .map(autoBid -> toAutoBidResponse(autoBid, auction))
                .toList());
    }

    @GetMapping("/me/auto-bids")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<List<AutoBidResponse>> listMyAutoBids() {
        return ResponseEntity.ok(autoBidService.listAutoBidsByUser(resolveCurrentUserId())
                .stream()
                .map(autoBid -> toAutoBidResponse(autoBid, auctionService.getAuction(autoBid.getAuctionId())))
                .toList());
    }

    @GetMapping("/me/wins")
    @PreAuthorize("hasRole('BIDDER')")
    public ResponseEntity<List<AuctionDto>> listMyWonAuctions() {
        return ResponseEntity.ok(
                auctionService.listWonAuctions(resolveCurrentUserId())
                        .stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    @GetMapping
    public ResponseEntity<AuctionPageResponse> listAuctions(@RequestParam(value = "category", required = false) String category,
                                                            @RequestParam(value = "q", required = false) String q,
                                                            @RequestParam(value = "state", required = false) String state,
                                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                                            @RequestParam(value = "size", defaultValue = "12") int size,
                                                            @RequestParam(value = "sort", defaultValue = "endTime,asc") String sort) {
        Sort sortSpec = parseSort(sort);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sortSpec);
        Page<Auction> result = auctionService.searchCatalog(category, q, parseState(state), pageable);

        return ResponseEntity.ok(toPageResponse(result));
    }

    @GetMapping("/trending")
    public ResponseEntity<AuctionPageResponse> listTrendingAuctions(@RequestParam(value = "category", required = false) String category,
                                                                    @RequestParam(value = "q", required = false) String q,
                                                                    @RequestParam(value = "state", required = false) String state,
                                                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                                                    @RequestParam(value = "size", defaultValue = "12") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Auction> result = auctionService.searchTrendingCatalog(category, q, parseState(state), pageable);
        return ResponseEntity.ok(toPageResponse(result));
    }

    @GetMapping("/for-you")
    public ResponseEntity<AuctionPageResponse> listPersonalizedAuctions(@RequestParam(value = "category", required = false) String category,
                                                                        @RequestParam(value = "q", required = false) String q,
                                                                        @RequestParam(value = "state", required = false) String state,
                                                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                                                        @RequestParam(value = "size", defaultValue = "12") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Auction> result = auctionService.searchPersonalizedCatalog(resolveCurrentUserIdOptional(), category, q, parseState(state), pageable);
        return ResponseEntity.ok(toPageResponse(result));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<AuctionDetailResponse> getAuctionDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(auctionService.getDetail(id));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> trackAuctionView(@PathVariable UUID id) {
        auctionService.incrementView(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDto> getAuction(@PathVariable UUID id) {
        return ResponseEntity.ok(toDto(auctionService.getAuction(id)));
    }

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(@PathVariable UUID id,
                                                             @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(limit == null ? bidService.getBidHistory(id) : bidService.getBidHistory(id, limit));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<AuctionSummaryResponse> getAuctionSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getAuctionSummary(id));
    }

    @GetMapping("/{id}/leader")
    public ResponseEntity<UUID> getCurrentLeader(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.getCurrentLeader(id));
    }

    @GetMapping("/config/min-increment")
    public ResponseEntity<Double> getMinIncrement() {
        return ResponseEntity.ok(bidService.getMinIncrement());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<Void> closeAuction(@PathVariable UUID id) {
        auctionService.closeAuction(id, "Auction closed by operator.");
        return ResponseEntity.noContent().build();
    }

    private AuctionDto toDto(Auction auction) {
        return toDto(auction, Map.of());
    }

    private AuctionDto toDto(Auction auction, Map<UUID, String> userNames) {
        if (auction == null) {
            return null;
        }

        AuctionDto dto = new AuctionDto();
        dto.id = auction.getId();
        dto.itemId = auction.getItemId();
        dto.itemName = auction.getTitle();
        dto.title = auction.getTitle();
        dto.description = auction.getDescription();
        dto.imageUrl = AuctionImageResolver.resolvePrimaryImage(auction);
        dto.category = auction.getCategory();
        dto.sellerId = auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy();
        dto.sellerName = auction.getSellerName() != null ? auction.getSellerName() : resolveUserName(userNames, dto.sellerId);
        dto.currentPrice = auction.getCurrentPrice();
        double reserve = auction.getReservePrice() == null ? 0.0 : auction.getReservePrice();
        dto.reservePrice = reserve;
        dto.reserveMet = reserve <= 0.0 || auction.getCurrentPrice() >= reserve;
        dto.viewCount = Math.max(0L, auction.getViewCount());
        dto.favoriteCount = auction.getFavoriteCount() == null ? 0L : auction.getFavoriteCount();
        dto.trendingScore = auction.getTrendingScore() == null ? 0.0 : auction.getTrendingScore();
        dto.bidCount = auction.getBidCount() == null ? 0 : auction.getBidCount();
        dto.minNextBid = auction.getMinNextBid() == null ? auction.getCurrentPrice() + bidService.getMinIncrement() : auction.getMinNextBid();
        dto.leaderId = auction.getLeaderId();
        dto.leaderName = resolveUserName(userNames, auction.getLeaderId());
        dto.winnerId = auction.getWinnerId();
        dto.winnerName = resolveUserName(userNames, auction.getWinnerId());
        dto.createdAt = auction.getCreatedAt();
        dto.startTime = auction.getStartTime();
        dto.endTime = auction.getEndTime();
        dto.state = auction.getState() == null ? null : auction.getState().name();
        return dto;
    }

    private BidPlacementResponse toBidPlacementResponse(BidTransaction bid, Auction auction) {
        BidPlacementResponse response = new BidPlacementResponse();
        response.setAuctionId(auction.getId());
        response.setBidId(bid.getId());
        response.setBidderId(bid.getBidderId());
        response.setBidderDisplay(auctionHelper.lookupUserName(bid.getBidderId()));
        response.setLeaderId(auction.getLeaderId());
        response.setLeaderName(auctionHelper.lookupUserName(auction.getLeaderId()));
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setMinNextBid(auction.getMinNextBid() == null ? auction.getCurrentPrice() + bidService.getMinIncrement() : auction.getMinNextBid());
        response.setState(auction.getState() == null ? null : auction.getState().name());
        response.setEndTime(auction.getEndTime());
        return response;
    }

    private AutoBidResponse toAutoBidResponse(AutoBid autoBid, Auction auction) {
        AutoBidResponse response = new AutoBidResponse();
        response.setAutoBidId(autoBid.getId());
        response.setAuctionId(autoBid.getAuctionId());
        response.setBidderId(autoBid.getBidderId());
        response.setBidderName(auctionHelper.lookupUserName(autoBid.getBidderId()));
        response.setMaxAmount(autoBid.getMaxAmount());
        response.setBidStep(autoBid.resolveBidStep(bidService.getMinIncrement()));
        response.setActive(autoBid.isActive());
        response.setAuctionState(auction.getState() == null ? null : auction.getState().name());
        response.setEndTime(auction.getEndTime());
        response.setCreatedAt(autoBid.getCreatedAt());
        response.setUpdatedAt(autoBid.getUpdatedAt());
        return response;
    }

    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is required");
        }

        User user = userService.findByUsername(auth.getName());
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user not found: " + auth.getName());
        }
        return user.getId();
    }

    private UUID resolveCurrentUserIdOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank() || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        User user = userService.findByUsername(auth.getName());
        return user == null ? null : user.getId();
    }

    private AuctionPageResponse toPageResponse(Page<Auction> result) {
        List<Auction> auctions = result.getContent();
        Map<UUID, String> userNames = auctionHelper.lookupUserNames(collectUserIds(auctions));
        AuctionPageResponse response = new AuctionPageResponse();
        response.setItems(auctions.stream().map(auction -> toDto(auction, userNames)).toList());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return response;
    }

    private boolean currentUserHasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + role.toUpperCase(Locale.ROOT);
        return auth.getAuthorities().stream().anyMatch(authority -> expected.equals(authority.getAuthority()));
    }

    private AuctionState parseState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        try {
            return AuctionState.valueOf(state.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unsupported auction state: " + state);
        }
    }

    private Sort parseSort(String sort) {
        String safeSort = sort == null || sort.isBlank() ? "endTime,asc" : sort;
        String[] parts = safeSort.split(",", 2);
        String property = switch (parts[0].trim()) {
            case "currentPrice", "title", "startTime", "endTime", "state", "category", "createdAt" -> parts[0].trim();
            default -> "endTime";
        };
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private List<UUID> collectUserIds(List<Auction> auctions) {
        List<UUID> ids = new ArrayList<>();
        for (Auction auction : auctions) {
            if (auction == null) {
                continue;
            }
            ids.add(auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy());
            ids.add(auction.getLeaderId());
            ids.add(auction.getWinnerId());
        }
        return ids;
    }

    private String resolveUserName(Map<UUID, String> userNames, UUID userId) {
        if (userId == null) {
            return null;
        }
        if (userNames != null && userNames.containsKey(userId)) {
            return userNames.get(userId);
        }
        return auctionHelper.lookupUserName(userId);
    }
}

