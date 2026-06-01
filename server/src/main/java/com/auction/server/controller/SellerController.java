package com.auction.server.controller;

import com.auction.server.dto.SellerStatsDto;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.ItemStatus;
import com.auction.server.entity.User;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seller")
public class SellerController {

    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final UserService userService;

    public SellerController(
            ItemRepository itemRepository,
            AuctionRepository auctionRepository,
            UserService userService
    ) {
        this.itemRepository = itemRepository;
        this.auctionRepository = auctionRepository;
        this.userService = userService;
    }

    @GetMapping("/stats")
    public ResponseEntity<SellerStatsDto> getStats(
            @RequestHeader(value = "X-Seller-Id", required = false) UUID sellerIdHeader,
            @RequestParam(value = "sellerId", required = false) UUID sellerIdParam) {

        UUID sellerId = resolveSellerId(sellerIdHeader, sellerIdParam);

        List<com.auction.server.entity.Auction> sellerAuctions = auctionRepository.findBySellerId(sellerId);
        long completedAuctions = sellerAuctions.stream()
                .filter(auction -> auction.getState() == AuctionState.FINISHED)
                .count();
        long wonAuctions = sellerAuctions.stream()
                .filter(auction -> auction.getState() == AuctionState.FINISHED && auction.getWinnerId() != null)
                .count();
        double totalRevenue = sellerAuctions.stream()
                .filter(auction -> auction.getState() == AuctionState.FINISHED && auction.getWinnerId() != null)
                .mapToDouble(com.auction.server.entity.Auction::getCurrentPrice)
                .sum();
        double averageSalePrice = wonAuctions == 0 ? 0.0 : totalRevenue / wonAuctions;
        double successRate = completedAuctions == 0 ? 0.0 : (wonAuctions * 100.0) / completedAuctions;

        SellerStatsDto stats = new SellerStatsDto(
                itemRepository.countBySellerId(sellerId),
                itemRepository.countBySellerIdAndStatus(sellerId, ItemStatus.PENDING),
                itemRepository.countBySellerIdAndStatus(sellerId, ItemStatus.APPROVED),
                itemRepository.countBySellerIdAndStatus(sellerId, ItemStatus.REJECTED),
                auctionRepository.countBySellerIdAndState(sellerId, AuctionState.ACTIVE),
                completedAuctions,
                successRate,
                totalRevenue,
                averageSalePrice
        );

        return ResponseEntity.ok(stats);
    }

    private UUID resolveSellerId(UUID sellerIdHeader, UUID sellerIdParam) {
        UUID authenticatedSellerId = resolveAuthenticatedUserIdOrNull();
        if (authenticatedSellerId != null) {
            return authenticatedSellerId;
        }

        UUID fallbackSellerId = sellerIdHeader != null ? sellerIdHeader : sellerIdParam;
        if (fallbackSellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }

        return fallbackSellerId;
    }

    private UUID resolveAuthenticatedUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        User user = userService.findByUsername(auth.getName());
        return user == null ? null : user.getId();
    }
}

