package com.auction.server.service.impl;

import com.auction.server.dto.ItemCreateDto;
import com.auction.server.dto.ItemCreateRequest;
import com.auction.server.dto.ItemDto;
import com.auction.server.dto.ItemResponse;
import com.auction.server.dto.PublicItemDetailDto;
import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.Item;
import com.auction.server.entity.ItemStatus;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.FavoriteRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ItemServiceImpl - mở rộng để hỗ trợ thao tác theo seller:
 * - findBySellerId (legacy returning ItemDto)
 * - createForSeller / updateForSeller / deleteForSeller (legacy ItemDto)
 * Đồng thời bổ sung API trả về ItemResponse (dùng cho frontend):
 * - findResponsesBySellerId
 * - createForSeller(UUID, ItemCreateRequest)
 * - updateForSeller(UUID, UUID, ItemCreateRequest)
 * - deleteForSellerResponse(UUID, UUID)
 */
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final AutoBidRepository autoBidRepository;
    private final FavoriteRepository favoriteRepository;
    private final DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;

    public ItemServiceImpl(ItemRepository itemRepository,
                           AuctionRepository auctionRepository,
                           AutoBidRepository autoBidRepository,
                           FavoriteRepository favoriteRepository) {
        this.itemRepository = itemRepository;
        this.auctionRepository = auctionRepository;
        this.autoBidRepository = autoBidRepository;
        this.favoriteRepository = favoriteRepository;
    }

    // -------------------------
    // Existing methods (kept)
    // -------------------------
    @Override
    @Transactional
    public Item createItem(ItemCreateDto dto, UUID sellerId) {
        if (dto == null) {
            throw new BusinessRuleException("ItemCreateDto is required");
        }
        if (dto.name == null || dto.name.trim().isEmpty()) {
            throw new BusinessRuleException("Item name is required");
        }
        if (dto.startPrice <= 0) {
            throw new BusinessRuleException("startPrice must be positive");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }

        Item item = new Item();
        item.setName(dto.name.trim());
        item.setDescription(dto.description == null ? "" : dto.description.trim());
        item.setStartingPrice(dto.startPrice);
        item.setSellerId(sellerId);
        item.setCreatedAt(Instant.now());

        return itemRepository.save(item);
    }

    @Override
    public Item getItem(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> new BusinessRuleException("Item not found: " + id));
    }

    @Override
    public PublicItemDetailDto getPublicItemDetail(UUID id) {
        Item item = getItem(id);
        PublicItemDetailDto dto = new PublicItemDetailDto();
        dto.setId(item.getId());
        dto.setProductName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setImagePath(item.getImagePath());
        dto.setImageUrls(itemImageUrls(item));
        dto.setSellerId(item.getSellerId());
        dto.setSku(item.getSku());
        dto.setQuantity(item.getQuantity());
        dto.setStatus(item.getStatus() == null ? null : item.getStatus().name());
        dto.setStartingPrice(item.getStartingPrice());
        return dto;
    }

    // -------------------------
    // Legacy DTO methods (ItemDto)
    // -------------------------
    /**
     * Trả về danh sách ItemDto của seller, sắp xếp theo createdAt desc.
     */
    @Override
    public List<ItemDto> findBySellerId(UUID sellerId) {
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        return itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Tạo item cho seller (dùng ItemDto để trả về).
     */
    @Override
    @Transactional
    public ItemDto createForSeller(ItemDto dto) {
        if (dto == null) {
            throw new BusinessRuleException("Item data is required");
        }
        if (dto.getSellerId() == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessRuleException("Item title is required");
        }
        if (dto.getStartingPrice() == null || dto.getStartingPrice() <= 0) {
            throw new BusinessRuleException("startingPrice must be positive");
        }

        Item item = new Item();
        item.setSellerId(dto.getSellerId());
        item.setName(dto.getTitle().trim());
        item.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        item.setStartingPrice(dto.getStartingPrice());
        item.setCreatedAt(Instant.now());

        Item saved = itemRepository.save(item);
        return toDto(saved);
    }

    /**
     * Cập nhật item cho seller: kiểm tra item tồn tại và sellerId khớp.
     */
    @Override
    @Transactional
    public ItemDto updateForSeller(UUID itemId, UUID sellerId, ItemDto dto) {
        if (itemId == null) {
            throw new BusinessRuleException("itemId is required");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));
        if (!sellerId.equals(item.getSellerId())) {
            throw new BusinessRuleException("Item not found for this seller");
        }

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            item.setName(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription().trim());
        }
        if (dto.getStartingPrice() != null && dto.getStartingPrice() > 0) {
            item.setStartingPrice(dto.getStartingPrice());
        }

        Item saved = itemRepository.save(item);
        return toDto(saved);
    }

    /**
     * Xóa item cho seller: kiểm tra item tồn tại và sellerId khớp.
     */
    @Override
    @Transactional
    public void deleteForSeller(UUID itemId, UUID sellerId) {
        if (itemId == null) {
            throw new BusinessRuleException("itemId is required");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));
        if (!sellerId.equals(item.getSellerId())) {
            throw new BusinessRuleException("Item not found for this seller");
        }
        Auction auction = auctionRepository.findByItemId(itemId).orElse(null);
        if (auction != null) {
            Instant now = Instant.now();
            if (!canSellerDeleteLinkedAuction(auction, now)) {
                throw new BusinessRuleException(resolveDeleteBlockedReason(auction, now));
            }

            cleanupLinkedAuctionData(auction.getId());
            auctionRepository.delete(auction);
        }
        itemRepository.delete(item);
    }

    // -------------------------
    // New methods for frontend contract (ItemCreateRequest / ItemResponse)
    // -------------------------
    /**
     * Trả về danh sách ItemResponse (format phù hợp frontend) của seller,
     * sắp xếp theo createdAt desc.
     */
    @Override
    public List<ItemResponse> findResponsesBySellerId(UUID sellerId) {
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        List<Item> items = itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        Map<UUID, Auction> auctionsByItemId = loadAuctionsByItemId(items);
        return items
                .stream()
                .map(item -> toResponse(item, auctionsByItemId.get(item.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemResponse> findRecentResponsesBySellerId(UUID sellerId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }

        List<Item> items = itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .limit(safeLimit)
                .toList();
        Map<UUID, Auction> auctionsByItemId = loadAuctionsByItemId(items);
        return items.stream()
                .map(item -> toResponse(item, auctionsByItemId.get(item.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Tạo item cho seller dựa trên request từ frontend, trả về ItemResponse.
     */
    @Transactional
    @Override
    public ItemResponse createForSeller(UUID sellerId, ItemCreateRequest request) {
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        if (request == null) {
            throw new BusinessRuleException("request is required");
        }
        if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
            throw new BusinessRuleException("productName is required");
        }
        if (request.getStartingPrice() == null || request.getStartingPrice() <= 0) {
            throw new BusinessRuleException("startingPrice must be positive");
        }
        validateQuantity(request.getQuantity());
        request.setStatus(null);

        Item item = new Item();
        item.setSellerId(sellerId);
        item.setName(request.getProductName().trim());
        item.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        item.setCategory(request.getCategory() == null || request.getCategory().isBlank()
                ? "General"
                : request.getCategory().trim());
        item.setStartingPrice(request.getStartingPrice());
        item.setReservePrice(request.getReservePrice() == null ? 0.0 : request.getReservePrice());
        item.setSku(request.getSku() == null ? null : request.getSku().trim());
        item.setQuantity(request.getQuantity() == null ? 1 : request.getQuantity());

        // status: map từ request nếu có, ngược lại mặc định PENDING (enum)
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            item.setStatus(ItemStatus.PENDING);
        } else {
            try {
                item.setStatus(ItemStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                // nếu không parse được, ném lỗi rõ ràng
                throw new BusinessRuleException("Invalid status value: " + request.getStatus());
            }
        }

        item.setStartTime(parseStartDate(request.getStartDate()));
        item.setEndTime(parseEndDate(request.getEndDate()));
        applyImages(item, request.getImagePath(), request.getImageUrls());
        item.setCreatedAt(Instant.now());

        Item saved = itemRepository.save(item);
        return toResponse(saved);
    }

    /**
     * Cập nhật item cho seller dựa trên request từ frontend, trả về ItemResponse.
     */
    @Transactional
    @Override
    public ItemResponse updateForSeller(UUID itemId, UUID sellerId, ItemCreateRequest request) {
        if (itemId == null) {
            throw new BusinessRuleException("itemId is required");
        }
        if (sellerId == null) {
            throw new BusinessRuleException("sellerId is required");
        }
        if (request == null) {
            throw new BusinessRuleException("request is required");
        }
        request.setStatus(null);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));
        if (!sellerId.equals(item.getSellerId())) {
            throw new BusinessRuleException("Item not found for this seller");
        }

        // A listing can only be edited before its auction goes live (still scheduled).
        Auction linkedAuction = auctionRepository.findByItemId(itemId).orElse(null);
        Instant editNow = Instant.now();
        if (linkedAuction != null && !canSellerEditLinkedAuction(linkedAuction, editNow)) {
            throw new BusinessRuleException(resolveEditBlockedReason(linkedAuction, editNow));
        }

        if (request.getProductName() != null && !request.getProductName().trim().isEmpty()) {
            item.setName(request.getProductName().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory().trim());
        }
        if (request.getStartingPrice() != null && request.getStartingPrice() > 0) {
            item.setStartingPrice(request.getStartingPrice());
        }
        if (request.getReservePrice() != null) {
            item.setReservePrice(request.getReservePrice());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                item.setStatus(ItemStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("Invalid status value: " + request.getStatus());
            }
        }
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            item.setStartTime(parseStartDate(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            item.setEndTime(parseEndDate(request.getEndDate()));
        }
        if (request.getImagePath() != null) {
            applyImages(item, request.getImagePath(), request.getImageUrls());
        } else if (request.getImageUrls() != null) {
            applyImages(item, item.getImagePath(), request.getImageUrls());
        }
        if (request.getSku() != null) {
            item.setSku(request.getSku().trim());
        }
        if (request.getQuantity() != null) {
            validateQuantity(request.getQuantity());
            item.setQuantity(request.getQuantity());
        }

        // Editing forces the listing back into admin review.
        item.setStatus(ItemStatus.PENDING);
        item.setApprovedBy(null);
        item.setApprovedAt(null);

        Item saved = itemRepository.save(item);

        // Discard the previously scheduled auction so the admin re-approves the edited
        // listing and recreates the auction with the updated details.
        if (linkedAuction != null) {
            cleanupLinkedAuctionData(linkedAuction.getId());
            auctionRepository.delete(linkedAuction);
        }

        return toResponse(saved);
    }

    /**
     * Xóa item cho seller (phiên bản trả về/điều khiển frontend).
     */
    @Transactional
    @Override
    public void deleteForSellerResponse(UUID itemId, UUID sellerId) {
        // reuse existing delete logic
        deleteForSeller(itemId, sellerId);
    }

    // -------------------------
    // Helpers: mapping
    // -------------------------
    private ItemDto toDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setSellerId(item.getSellerId());
        dto.setTitle(item.getName());
        dto.setDescription(item.getDescription());
        dto.setStartingPrice(item.getStartingPrice());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }

    private ItemResponse toResponse(Item item) {
        return toResponse(item, null);
    }

    private ItemResponse toResponse(Item item, Auction auction) {
        if (item == null) return null;

        ItemResponse r = new ItemResponse();
        r.setSellerId(item.getSellerId());
        r.setSku(item.getSku());
        r.setQuantity(item.getQuantity());
        r.setId(item.getId());
        r.setProductName(item.getName());
        r.setDescription(item.getDescription());
        r.setCategory(item.getCategory());
        r.setStartingPrice(item.getStartingPrice() == null ? 0.0 : item.getStartingPrice());
        r.setReservePrice(item.getReservePrice());

        // Convert enum -> String (safe null check)
        if (item.getStatus() != null) {
            r.setStatus(item.getStatus().name());
        } else {
            r.setStatus(null);
        }

        r.setImagePath(item.getImagePath());
        r.setImageUrls(itemImageUrls(item));
        r.setStartDate(formatDate(item.getStartTime()));
        r.setEndDate(formatDate(item.getEndTime()));
        Instant now = Instant.now();
        boolean deletable = auction == null || canSellerDeleteLinkedAuction(auction, now);
        r.setDeletable(deletable);
        r.setDeleteBlockedReason(deletable
                ? null
                : resolveDeleteBlockedReason(auction, now));
        boolean editable = auction == null || canSellerEditLinkedAuction(auction, now);
        r.setEditable(editable);
        r.setEditBlockedReason(editable
                ? null
                : resolveEditBlockedReason(auction, now));
        return r;
    }

    private Map<UUID, Auction> loadAuctionsByItemId(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }

        List<UUID> itemIds = items.stream()
                .map(Item::getId)
                .filter(Objects::nonNull)
                .toList();

        if (itemIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Auction> result = new HashMap<>();
        for (Auction auction : auctionRepository.findByItemIdIn(itemIds)) {
            if (auction == null || auction.getItemId() == null) {
                continue;
            }

            Auction existing = result.get(auction.getItemId());
            if (existing == null || compareAuctionFreshness(auction, existing) > 0) {
                result.put(auction.getItemId(), auction);
            }
        }

        return result;
    }

    private int compareAuctionFreshness(Auction left, Auction right) {
        return Comparator
                .comparing(Auction::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Auction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(left, right);
    }

    private Instant parseStartDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // nếu frontend gửi ISO-8601 (ví dụ "2026-05-05T23:00:00Z")
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            // thử parse dạng yyyy-MM-dd (date-only)
            try {
                LocalDate d = LocalDate.parse(value);
                return d.atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ex2) {
                throw new IllegalArgumentException("Invalid startDate format: " + value, ex2);
            }
        }
    }

    private Instant parseEndDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        // ưu tiên ISO-8601 nếu frontend gửi timestamp
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            // nếu frontend gửi date-only (yyyy-MM-dd), interpret as end of day UTC
            try {
                LocalDate d = LocalDate.parse(value);
                return d.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ex2) {
                throw new IllegalArgumentException("Invalid endDate format: " + value, ex2);
            }
        }
    }

    private String formatDate(Instant value) {
        if (value == null) {
            return null;
        }
        // trả về yyyy-MM-dd (frontend-friendly)
        return LocalDate.ofInstant(value, ZoneOffset.UTC).toString();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity != null && quantity < 0) {
            throw new BusinessRuleException("quantity must be zero or greater");
        }
    }

    private void applyImages(Item item, String primaryImage, List<String> imageUrls) {
        List<String> normalizedUrls = normalizeImageUrls(primaryImage, imageUrls);
        item.setImagePath(normalizedUrls.isEmpty() ? "" : normalizedUrls.get(0));
        item.setImageUrls(normalizedUrls);
    }

    private List<String> itemImageUrls(Item item) {
        return normalizeImageUrls(item.getImagePath(), item.getImageUrls());
    }

    private List<String> normalizeImageUrls(String primaryImage, List<String> imageUrls) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        addImageReference(normalized, primaryImage);

        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                addImageReference(normalized, imageUrl);
            }
        }

        return new ArrayList<>(normalized);
    }

    private void addImageReference(LinkedHashSet<String> normalized, String imageReference) {
        if (imageReference == null || imageReference.isBlank()) {
            return;
        }

        String trimmed = imageReference.trim();
        validateImageReference(trimmed);
        normalized.add(trimmed);
    }

    private void validateImageReference(String imageReference) {
        boolean looksLikeWindowsPath = imageReference.matches("^[A-Za-z]:\\\\.*");
        boolean looksLikeFileUri = imageReference.startsWith("file:");
        boolean looksLikeUncPath = imageReference.startsWith("\\\\");

        if (looksLikeWindowsPath || looksLikeFileUri || looksLikeUncPath) {
            throw new BusinessRuleException("Image references must be uploaded URLs, not local file paths");
        }
    }

    private boolean canSellerDeleteLinkedAuction(Auction auction, Instant now) {
        if (auction == null) {
            return true;
        }

        if (auction.getState() != AuctionState.DRAFT && auction.getState() != AuctionState.SCHEDULED) {
            return false;
        }

        return auction.getStartTime() == null || now.isBefore(auction.getStartTime());
    }

    private String resolveDeleteBlockedReason(Auction auction, Instant now) {
        if (auction == null) {
            return null;
        }

        if (canSellerDeleteLinkedAuction(auction, now)) {
            return null;
        }

        if (auction.getState() == AuctionState.ACTIVE) {
            return "This listing cannot be deleted because the auction is already live.";
        }

        if (auction.getState() == AuctionState.FINISHED
                || auction.getState() == AuctionState.CANCELLED
                || auction.getState() == AuctionState.REJECTED
                || auction.getState() == AuctionState.DELETED) {
            return "This listing cannot be deleted because the auction has already ended.";
        }

        return "This listing can only be deleted before the auction start time.";
    }

    private boolean canSellerEditLinkedAuction(Auction auction, Instant now) {
        // Same rule as deletion: editing is only allowed while the auction is still
        // scheduled (or draft) and has not started yet.
        return canSellerDeleteLinkedAuction(auction, now);
    }

    private String resolveEditBlockedReason(Auction auction, Instant now) {
        if (auction == null) {
            return null;
        }

        if (canSellerEditLinkedAuction(auction, now)) {
            return null;
        }

        if (auction.getState() == AuctionState.ACTIVE) {
            return "This listing cannot be edited because the auction is already live.";
        }

        if (auction.getState() == AuctionState.FINISHED
                || auction.getState() == AuctionState.CANCELLED
                || auction.getState() == AuctionState.REJECTED
                || auction.getState() == AuctionState.DELETED) {
            return "This listing cannot be edited because the auction has already ended.";
        }

        return "This listing can only be edited before the auction start time.";
    }

    private void cleanupLinkedAuctionData(UUID auctionId) {
        if (auctionId == null) {
            return;
        }

        autoBidRepository.deleteByAuctionId(auctionId);
        favoriteRepository.deleteByAuctionId(auctionId);
    }
}

