package com.team.backend.service.impl;

import com.team.backend.dto.PendingItemDto;
import com.team.backend.dto.AdminWalletActivityDto;
import com.team.backend.dto.AdminNotificationDto;
import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.Item;
import com.team.backend.entity.ItemStatus;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.ItemRepository;

import com.team.backend.service.AdminService;
import com.team.backend.dto.AdminStatsDto;
import com.team.backend.repository.UserRepository;
import com.team.backend.repository.WalletTransactionRepository;
import com.team.backend.repository.AdminNotificationRepository;
import com.team.backend.entity.AdminNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AdminNotificationRepository adminNotificationRepository;

    public AdminServiceImpl(
            ItemRepository itemRepository,
            AuctionRepository auctionRepository,
            UserRepository userRepository,
            WalletTransactionRepository walletTransactionRepository,
            AdminNotificationRepository adminNotificationRepository
    ) {
        this.itemRepository = itemRepository;
        this.auctionRepository = auctionRepository;
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
        return items.stream().map(this::toPendingDto).collect(Collectors.toList());
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
        if (startingPrice <= 0) {
            throw new BusinessRuleException("startingPrice must be positive");
        }
        if (reservePrice != null && reservePrice < 0) {
            throw new BusinessRuleException("reservePrice must be non-negative");
        }
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BusinessRuleException("start must be before end");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));

        if (item.getStatus() != ItemStatus.APPROVED) {
            throw new BusinessRuleException("Item must be APPROVED before creating auction");
        }

        Auction auction = new Auction();
        auction.setItem(item);
        auction.setItemId(item.getId());
        auction.setStartTime(start == null ? Instant.now() : start);
        auction.setEndTime(end == null ? Instant.now().plusSeconds(3600) : end);
        auction.setCreatedBy(adminId);
        auction.setCreatedAt(Instant.now());
        auction.setCurrentPrice(startingPrice);
        auction.setReservePrice(reservePrice == null ? 0.0 : reservePrice);

        Instant now = Instant.now();
        if (start != null && start.isAfter(now)) {
            auction.setState(AuctionState.SCHEDULED);
        } else {
            auction.setState(AuctionState.ACTIVE);
        }

        Auction saved = auctionRepository.save(auction);
        saveNotification("AUCTION_CREATED", "Auction created for item: " + item.getName());
        log.info("Auction created for item: itemId={}, auctionId={}, adminId={}", itemId, saved.getId(), adminId);
        return saved;
    }

    /**
     * Helper: map Item -> PendingItemDto
     */
    private PendingItemDto toPendingDto(Item item) {
        PendingItemDto d = new PendingItemDto();
        d.id = item.getId();
        d.sellerId = item.getSellerId();
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
    @Transactional(readOnly = true)
    public List<PendingItemDto> listReportedItems() {
        return itemRepository.findByStatus(ItemStatus.REJECTED)
                .stream()
                .map(this::toPendingDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long totalUsers = userRepository.count();
        long activeSellers = userRepository.findByRole("SELLER").size();
        long totalAuctions = auctionRepository.count();

        double revenue = auctionRepository.findByState(AuctionState.FINISHED)
                .stream()
                .filter(a -> a.getWinnerId() != null)
                .mapToDouble(Auction::getCurrentPrice)
                .sum();

        return new AdminStatsDto(totalUsers, activeSellers, totalAuctions, revenue);
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
}
