package com.auction.server.service.impl;

import com.auction.server.dto.PendingItemDto;
import com.auction.server.dto.AdminWalletActivityDto;
import com.auction.server.dto.AdminNotificationDto;
import com.auction.server.dto.AuctionCreateDto;
import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.Item;
import com.auction.server.entity.ItemStatus;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.ItemRepository;

import com.auction.server.service.AdminService;
import com.auction.server.service.AuctionHelper;
import com.auction.server.service.AuctionService;
import com.auction.server.dto.AdminStatsDto;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.server.repository.AdminNotificationRepository;
import com.auction.server.entity.AdminNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AdminServiceImpl - triển khai các thao tác dành cho admin:
 * - listPendingItems: trả danh sách item chờ duyệt
 * - approveItem: duyệt item
 * - createAuctionForItem: tạo auction cho item đã duyệt
 * Lưu ý: nếu Item.status là String thay vì enum, điều chỉnh repository và so sánh tương ứng.
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;
    private final AuctionHelper auctionHelper;

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AdminNotificationRepository adminNotificationRepository;

    public AdminServiceImpl(
            ItemRepository itemRepository,
            AuctionRepository auctionRepository,
            AuctionService auctionService,
            AuctionHelper auctionHelper,
            UserRepository userRepository,
            WalletTransactionRepository walletTransactionRepository,
            AdminNotificationRepository adminNotificationRepository
    ) {
        this.itemRepository = itemRepository;
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
        this.auctionHelper = auctionHelper;
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.adminNotificationRepository = adminNotificationRepository;
    }

    /**
     * Trả về danh sách item có trạng thái PENDING (chờ duyệt).
     * Nếu số lượng lớn, nên mở rộng với paging (Pageable).
     */
    @Override
    @Transactional(readOnly = true)
    public List<PendingItemDto> listPendingItems() {
        List<Item> items = itemRepository.findByStatus(ItemStatus.PENDING);
        Map<UUID, String> sellerNames = loadSellerNames(items);
        return items.stream().map(item -> toPendingDto(item, sellerNames)).collect(Collectors.toList());
    }

    /**
     * Approve an item (set status APPROVED, approvedBy, approvedAt).
     */
    @Override
    @Transactional
    public Item approveItem(UUID itemId, UUID adminId) {
        if (itemId == null) throw new BusinessRuleException("itemId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        if (item.getStatus() == ItemStatus.APPROVED) {
            log.debug("approveItem: item {} already approved", itemId);
            return item; // idempotent
        }

        item.setStatus(ItemStatus.APPROVED);
        item.setApprovedBy(adminId);
        item.setApprovedAt(Instant.now());

        Item saved = itemRepository.save(item);
        saveNotification("ITEM_APPROVED", "Item approved: " + saved.getName());
        log.info("Item approved: itemId={}, adminId={}", itemId, adminId);
        return saved;
    }

    /**
     * Create an auction for an approved item.
     * Signature: (UUID itemId, Instant start, Instant end, UUID adminId, double startingPrice, Double reservePrice)
     */
    @Override
    @Transactional
    public Auction createAuctionForItem(UUID itemId,
                                        Instant start,
                                        Instant end,
                                        UUID adminId,
                                        double startingPrice,
                                        Double reservePrice) {

        if (itemId == null) throw new BusinessRuleException("itemId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");
        if (startingPrice < 0) {
            throw new BusinessRuleException("startingPrice must not be negative");
        }
        if (reservePrice != null && reservePrice < 0) {
            throw new BusinessRuleException("reservePrice must be non-negative");
        }
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BusinessRuleException("start must be before end");
        }

        return createAuctionForItemInternal(itemId, start, end, adminId, startingPrice, reservePrice, true);
    }

    @Override
    @Transactional
    public Auction approveAndCreateAuction(UUID itemId,
                                           Instant start,
                                           Instant end,
                                           UUID adminId,
                                           double startingPrice,
                                           Double reservePrice) {
        return createAuctionForItemInternal(itemId, start, end, adminId, startingPrice, reservePrice, true);
    }

    private Auction createAuctionForItemInternal(UUID itemId,
                                                 Instant start,
                                                 Instant end,
                                                 UUID adminId,
                                                 double startingPrice,
                                                 Double reservePrice,
                                                 boolean autoApprovePending) {
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        if (auctionRepository.existsByItemId(itemId)) {
            throw new BusinessRuleException("Auction already exists for item: " + itemId);
        }

        if (item.getStatus() == ItemStatus.PENDING && autoApprovePending) {
            item.setStatus(ItemStatus.APPROVED);
            item.setApprovedBy(adminId);
            item.setApprovedAt(Instant.now());
            itemRepository.save(item);
            saveNotification("ITEM_APPROVED", "Item approved: " + item.getName());
        } else if (item.getStatus() != ItemStatus.APPROVED) {
            throw new BusinessRuleException("Item must be APPROVED before creating auction");
        }

        AuctionCreateDto dto = new AuctionCreateDto();
        dto.setItemId(item.getId());
        dto.setStartTime(start == null ? Instant.now() : start);
        dto.setEndTime(end == null ? Instant.now().plusSeconds(3600) : end);
        double effectiveStartingPrice = startingPrice > 0
                ? startingPrice
                : (item.getStartingPrice() == null ? 0.0 : item.getStartingPrice());
        if (effectiveStartingPrice <= 0) {
            throw new BusinessRuleException("startingPrice must be positive");
        }
        dto.setStartPrice(effectiveStartingPrice);
        dto.setReservePrice(reservePrice == null ? item.getReservePrice() : reservePrice);

        Auction saved = auctionService.createAuction(dto, item.getSellerId(), adminId);
        saveNotification("AUCTION_CREATED", "Auction created for item: " + item.getName());
        log.info("Auction created for item: itemId={}, auctionId={}, adminId={}", itemId, saved.getId(), adminId);
        return saved;
    }

    /**
     * Helper: map Item -> PendingItemDto
     */
    private PendingItemDto toPendingDto(Item item) {
        return toPendingDto(item, Map.of());
    }

    private PendingItemDto toPendingDto(Item item, Map<UUID, String> sellerNames) {
        PendingItemDto d = new PendingItemDto();
        d.id = item.getId();
        d.sellerId = item.getSellerId();
        d.sellerName = sellerNames.getOrDefault(item.getSellerId(), auctionHelper.lookupUserName(item.getSellerId()));
        d.productName = item.getName();
        d.description = item.getDescription();
        d.category = item.getCategory();
        d.startingPrice = item.getStartingPrice();
        d.reservePrice = item.getReservePrice();
        d.status = item.getStatus() == null ? null : item.getStatus().name();
        d.imagePath = item.getImagePath();
        d.startDate = item.getStartTime() == null ? null : item.getStartTime().toString();
        d.endDate = item.getEndTime() == null ? null : item.getEndTime().toString();
        return d;
    }

    @Override
    @Transactional
    public Item rejectItem(UUID itemId, UUID adminId) {
        if (itemId == null) throw new BusinessRuleException("itemId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        item.setStatus(ItemStatus.REJECTED);
        Item saved = itemRepository.save(item);
        saveNotification("ITEM_REJECTED", "Item rejected: " + saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public void deleteItem(UUID itemId) {
        if (itemId == null) throw new BusinessRuleException("itemId is required");

        if (auctionRepository.existsByItemId(itemId)) {
            throw new BusinessRuleException("Cannot delete item because it already has auction");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        itemRepository.delete(item);
        saveNotification("ITEM_DELETED", "Item deleted: " + item.getName());
    }

    @Override
    @Transactional
    public void acceptAuction(UUID auctionId, UUID adminId) {
        if (auctionId == null) throw new BusinessRuleException("auctionId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessRuleException("Auction not found: " + auctionId));

        auctionService.acceptAuction(auctionId);
        saveNotification("AUCTION_ACCEPTED", "Auction accepted: " + auction.getTitle());
    }

    @Override
    @Transactional
    public void rejectAuction(UUID auctionId, UUID adminId) {
        if (auctionId == null) throw new BusinessRuleException("auctionId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessRuleException("Auction not found: " + auctionId));

        auctionService.rejectAuction(auctionId, "Auction rejected by admin.");

        // Keep the seller-facing item status in sync with the auction decision so the
        // seller's listing no longer shows "approved" after the auction was rejected.
        syncItemStatus(auction.getItemId(), ItemStatus.REJECTED);

        saveNotification("AUCTION_REJECTED", "Auction rejected: " + auction.getTitle());
    }

    private void syncItemStatus(UUID itemId, ItemStatus status) {
        if (itemId == null) {
            return;
        }
        itemRepository.findById(itemId).ifPresent(item -> {
            if (item.getStatus() != status) {
                item.setStatus(status);
                if (status != ItemStatus.APPROVED) {
                    item.setApprovedBy(null);
                    item.setApprovedAt(null);
                }
                itemRepository.save(item);
            }
        });
    }

    @Override
    @Transactional
    public void deleteAuction(UUID auctionId, UUID adminId) {
        if (auctionId == null) throw new BusinessRuleException("auctionId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessRuleException("Auction not found: " + auctionId));

        auctionService.deleteAuction(auctionId);
        saveNotification("AUCTION_DELETED", "Auction marked deleted: " + auction.getTitle());
    }

    @Override
    @Transactional
    public void purgeAuction(UUID auctionId, UUID adminId) {
        if (auctionId == null) throw new BusinessRuleException("auctionId is required");
        if (adminId == null) throw new BusinessRuleException("adminId is required");

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessRuleException("Auction not found: " + auctionId));

        String title = auction.getTitle();
        auctionService.purgeAuction(auctionId);
        saveNotification("AUCTION_PURGED", "Auction permanently removed: " + title);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingItemDto> listReportedItems() {
        List<Item> items = itemRepository.findByStatus(ItemStatus.REJECTED);
        Map<UUID, String> sellerNames = loadSellerNames(items);
        return items
                .stream()
                .map(item -> toPendingDto(item, sellerNames))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long totalUsers = userRepository.count();
        long activeSellers = userRepository.findByRole("SELLER").size();
        long totalAuctions = auctionRepository.count();
        long activeAuctions = auctionRepository.findByState(AuctionState.ACTIVE).size();
        long closedAuctions = auctionRepository.findByState(AuctionState.FINISHED).size();
        Instant monthStart = Instant.now().atZone(ZoneOffset.UTC).withDayOfMonth(1).toInstant();
        long newSellersThisMonth = userRepository.findByRole("SELLER").stream()
                .filter(user -> user.getCreatedAt() != null && !user.getCreatedAt().isBefore(monthStart))
                .count();

        double revenue = auctionRepository.findByState(AuctionState.FINISHED)
                .stream()
                .filter(a -> a.getWinnerId() != null)
                .mapToDouble(Auction::getCurrentPrice)
                .sum();

        double auctionSuccessRate = closedAuctions == 0
                ? 0.0
                : (auctionRepository.findByState(AuctionState.FINISHED).stream().filter(a -> a.getWinnerId() != null).count() * 100.0) / closedAuctions;

        return new AdminStatsDto(
                totalUsers,
                activeSellers,
                totalAuctions,
                activeAuctions,
                closedAuctions,
                newSellersThisMonth,
                auctionSuccessRate,
                revenue
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminWalletActivityDto> getRecentWalletActivity(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return walletTransactionRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .limit(safeLimit)
                .map(tx -> {
                    AdminWalletActivityDto dto = new AdminWalletActivityDto();
                    dto.setId(tx.getId());
                    dto.setUserId(tx.getUserId());
                    dto.setUserDisplayName(auctionHelper.lookupUserName(tx.getUserId()));
                    dto.setType(tx.getType());
                    dto.setAmount(tx.getAmount());
                    dto.setBalanceAfter(tx.getBalanceAfter());
                    dto.setCreatedAt(tx.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotificationDto> getRecentNotifications(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return adminNotificationRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .limit(safeLimit)
                .map(notification -> {
                    AdminNotificationDto dto = new AdminNotificationDto();
                    dto.setId(notification.getId());
                    dto.setType(notification.getType());
                    dto.setMessage(notification.getMessage());
                    dto.setCreatedAt(notification.getCreatedAt());
                    dto.setRead(notification.isRead());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void saveNotification(String type, String message) {
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setCreatedAt(Instant.now());
        notification.setRead(false);
        adminNotificationRepository.save(notification);
    }

    private Map<UUID, String> loadSellerNames(Collection<Item> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }

        return auctionHelper.lookupUserNames(items.stream()
                .map(Item::getSellerId)
                .filter(Objects::nonNull)
                .toList());
    }
}

