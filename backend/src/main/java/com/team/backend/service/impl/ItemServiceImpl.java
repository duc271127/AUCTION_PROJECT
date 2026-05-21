package com.team.backend.service.impl;

import com.team.backend.dto.ItemCreateDto;
import com.team.backend.dto.ItemCreateRequest;
import com.team.backend.dto.ItemDto;
import com.team.backend.dto.ItemResponse;
import com.team.backend.entity.Item;
import com.team.backend.entity.ItemStatus;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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
    private final DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;

    public ItemServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
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
        return itemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toResponse)
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
        item.setImagePath(request.getImagePath() == null ? "" : request.getImagePath());
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

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Item not found: " + itemId));
        if (!sellerId.equals(item.getSellerId())) {
            throw new BusinessRuleException("Item not found for this seller");
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
            item.setImagePath(request.getImagePath());
        }
        if (request.getSku() != null) {
            item.setSku(request.getSku().trim());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }

        Item saved = itemRepository.save(item);
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
        r.setStartDate(formatDate(item.getStartTime()));
        r.setEndDate(formatDate(item.getEndTime()));
        return r;
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
}
