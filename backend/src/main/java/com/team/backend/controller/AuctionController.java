package com.team.backend.controller;

import com.team.backend.dto.AuctionCreateDto;
import com.team.backend.dto.AuctionDto;
import com.team.backend.dto.AutoBidRequestDto;
import com.team.backend.dto.BidHistoryDto;
import com.team.backend.dto.BidRequestDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AutoBid;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.realtime.RealtimeNotifier;
import com.team.backend.service.AuctionService;
import com.team.backend.service.AutoBidService;
import com.team.backend.service.BidService;
import com.team.backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuctionController - controller đầy đủ cho các thao tác liên quan auction:
 * - tạo auction (seller/admin)
 * - đặt giá (authenticated)
 * - đặt auto-bid (authenticated)
 * - liệt kê auctions, xem chi tiết auction
 * - đóng auction (authenticated)
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
    private final UserService userService;
    private final RealtimeNotifier realtimeNotifier;
    private final AutoBidService autoBidService;

    public AuctionController(AuctionService auctionService,
                             BidService bidService,
                             UserService userService,
                             RealtimeNotifier realtimeNotifier,
                             AutoBidService autoBidService) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.userService = userService;
        this.realtimeNotifier = realtimeNotifier;
        this.autoBidService = autoBidService;
    }

    // -------------------------
    // Tạo auction
    // -------------------------

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AuctionDto> createAuction(@Valid @RequestBody AuctionCreateDto dto) {
        UUID sellerId = resolveUserIdFromSecurity();
        Auction created = auctionService.createAuction(dto, sellerId);
        URI location = URI.create("/api/auctions/" + created.getId());
        log.info("Đã tạo auction bởi seller {}: auctionId={}", sellerId, created.getId());
        return ResponseEntity.created(location).body(toDto(created));
    }

    @PostMapping("/seller/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuctionDto> createAuctionWithSeller(@PathVariable UUID sellerId,
                                                              @Valid @RequestBody AuctionCreateDto dto) {
        Auction created = auctionService.createAuction(dto, sellerId);
        URI location = URI.create("/api/auctions/" + created.getId());
        log.info("Admin {} đã tạo auction cho seller {}: auctionId={}", resolveUserIdFromSecurity(), sellerId, created.getId());
        return ResponseEntity.created(location).body(toDto(created));
    }

    // -------------------------
    // Đặt giá
    // -------------------------

    /**
     * Đặt giá cho một auction.
     * - bidderId có thể được truyền qua header X-User-Id (dùng cho testing) hoặc lấy từ security context.
     * - body chứa amount (và có thể bidderId cho luồng test không auth).
     * - trả về AuctionDto đã cập nhật.
     */
    @PostMapping("/{id}/bids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuctionDto> placeBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody BidRequestDto dto) {

        UUID bidderId = userId != null ? userId : dto.bidderId;
        if (bidderId == null) {
            throw new BusinessRuleException("Yêu cầu phải có bidderId");
        }

        // đọc trước để lấy endTime cũ phục vụ kiểm tra anti-sniping
        Auction beforeBid = auctionService.getAuction(id);
        Instant oldEndTime = beforeBid == null ? null : beforeBid.getEndTime();

        // thực hiện đặt giá (service xử lý validate, lock, auto-bid, anti-sniping)
        bidService.placeBid(id, bidderId, dto.amount);

        // lấy auction đã cập nhật và phát sự kiện realtime
        Auction updated = auctionService.getAuction(id);

        // phát sự kiện đặt giá
        RealtimeEvent bidEvent = RealtimeEventFactory.bidPlaced(
                id,
                bidderId,
                updated.getCurrentPrice(),
                updated.getEndTime()
        );
        realtimeNotifier.broadcastToAuction(id, bidEvent);

        // nếu auction được gia hạn do anti-sniping thì phát sự kiện gia hạn
        if (oldEndTime != null && updated.getEndTime() != null && updated.getEndTime().isAfter(oldEndTime)) {
            RealtimeEvent extendedEvent = RealtimeEventFactory.auctionExtended(
                    id,
                    updated.getCurrentPrice(),
                    updated.getEndTime()
            );
            realtimeNotifier.broadcastToAuction(id, extendedEvent);
        }

        log.info("Đã đặt giá: auction={}, bidder={}, amount={}", id, bidderId, dto.amount);
        return ResponseEntity.ok(toDto(updated));
    }

    // -------------------------
    // Auto-bid
    // -------------------------

    @PostMapping("/{id}/auto-bid")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AutoBid> setAutoBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody AutoBidRequestDto dto) {

        UUID bidderId = userId != null ? userId : dto.bidderId;
        if (bidderId == null) {
            throw new BusinessRuleException("Yêu cầu phải có bidderId");
        }

        AutoBid autoBid = autoBidService.setAutoBid(id, bidderId, dto.maxAmount);
        log.info("Đã thiết lập AutoBid: auction={}, bidder={}, maxAmount={}", id, bidderId, dto.maxAmount);
        return ResponseEntity.ok(autoBid);
    }

    // -------------------------
    // Đọc dữ liệu liên quan auto-bid
    // -------------------------

    @DeleteMapping("/{id}/auto-bid")
    public ResponseEntity<Void> cancelAutoBid(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        UUID bidderId = userId;
        if (bidderId == null) {
            throw new BusinessRuleException("Yêu cầu phải có bidderId");
        }

        autoBidService.cancelAutoBid(id, bidderId);
        log.info("Đã huỷ AutoBid: auction={}, bidder={}", id, bidderId);
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
            throw new BusinessRuleException("Yêu cầu phải có bidderId");
        }
        List<AutoBid> list = autoBidService.listAutoBidsByUser(userId);
        return ResponseEntity.ok(list);
    }

    // =========================
    // Danh sách và chi tiết auction
    // =========================

    @GetMapping
    public ResponseEntity<List<AuctionDto>> listAuctions() {
        List<Auction> auctions = auctionService.listAuctions();
        List<AuctionDto> dtos = auctions.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDto> getAuction(@PathVariable UUID id) {
        Auction a = auctionService.getAuction(id);
        return ResponseEntity.ok(toDto(a));
    }

    // Lịch sử đặt giá (trả BidHistoryDto)
    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidHistoryDto>> getBidHistory(
            @PathVariable UUID id,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        if (limit == null) {
            return ResponseEntity.ok(bidService.getBidHistory(id));
        } else {
            return ResponseEntity.ok(bidService.getBidHistory(id, limit));
        }
    }

    // =========================
    // Thông tin tóm tắt / metadata
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

    // -------------------------
    // Admin / điều khiển
    // -------------------------

    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> closeAuction(@PathVariable UUID id) {
        auctionService.closeAuction(id);
        // phát sự kiện auction kết thúc
        RealtimeEvent finished = RealtimeEventFactory.auctionFinished(id);
        realtimeNotifier.broadcastToAuction(id, finished);
        log.info("Đã đóng auction: auctionId={}", id);
        return ResponseEntity.noContent().build();
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
            throw new BusinessRuleException("Người dùng chưa xác thực, không thể xác định user");
        }
        String username = auth.getName();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng đã xác thực: " + username);
        }
        return user.getId();
    }
}
