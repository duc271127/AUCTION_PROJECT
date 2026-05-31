package com.team.backend.service;

import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.BidTransaction;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class AuctionHelper {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AutoBidRepository autoBidRepository;
    private final UserRepository userRepository;
    private final Map<UUID, String> userNameCache = new ConcurrentHashMap<>();

    public AuctionHelper(AuctionRepository auctionRepository,
                         BidRepository bidRepository,
                         AutoBidRepository autoBidRepository,
                         UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.autoBidRepository = autoBidRepository;
        this.userRepository = userRepository;
    }

    public String lookupUserName(UUID userId) {
        if (userId == null) {
            return null;
        }

        return userNameCache.computeIfAbsent(userId, this::loadUserName);
    }

    public Map<UUID, String> lookupUserNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<UUID> uniqueIds = userIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<UUID> missingIds = uniqueIds.stream()
                .filter(id -> !userNameCache.containsKey(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!missingIds.isEmpty()) {
            userRepository.findAllById(missingIds).forEach(user ->
                    userNameCache.put(user.getId(), resolveUserName(user.getId(), user.getDisplayName(), user.getUsername(), user.getEmail())));

            missingIds.stream()
                    .filter(id -> !userNameCache.containsKey(id))
                    .forEach(id -> userNameCache.put(id, shortId(id)));
        }

        return uniqueIds.stream().collect(Collectors.toMap(id -> id, this::lookupUserName));
    }

    public long computeRemainingSeconds(UUID auctionId) {
        if (auctionId == null) {
            return 0L;
        }

        Optional<Auction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) {
            return 0L;
        }

        Auction auction = opt.get();
        if (auction.getEndTime() == null) {
            return 0L;
        }

        long seconds = Duration.between(Instant.now(), auction.getEndTime()).getSeconds();
        return Math.max(0L, seconds);
    }

    public List<BidHistoryDto> toBidHistoryItems(UUID auctionId) {
        return bidRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId)
                .stream()
                .map(this::toBidHistoryItem)
                .toList();
    }

    public BidHistoryDto toBidHistoryItem(UUID bidderId, double amount, Instant createdAt) {
        BidHistoryDto dto = new BidHistoryDto();
        dto.setBidderId(bidderId);
        dto.setBidderName(lookupUserName(bidderId));
        dto.setAmount(amount);
        dto.setCreatedAt(createdAt);
        return dto;
    }

    public BidHistoryDto toBidHistoryItem(BidTransaction tx) {
        BidHistoryDto dto = toBidHistoryItem(tx.getBidderId(), tx.getAmount(), tx.getCreatedAt());
        dto.setBidId(tx.getId());
        dto.setAuctionId(tx.getAuctionId());
        boolean autoBid = autoBidRepository.existsByAuctionIdAndBidderIdAndActiveTrue(tx.getAuctionId(), tx.getBidderId());
        dto.setAutoBid(autoBid);
        dto.setSource(autoBid ? "AUTO_BID" : "MANUAL_BID");
        return dto;
    }

    private String shortId(UUID userId) {
        String value = userId.toString().replace("-", "");
        return value.length() > 8 ? value.substring(0, 8) : value;
    }

    private String loadUserName(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> resolveUserName(userId, user.getDisplayName(), user.getUsername(), user.getEmail()))
                .orElseGet(() -> shortId(userId));
    }

    private String resolveUserName(UUID userId, String displayName, String username, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        return shortId(userId);
    }
}
