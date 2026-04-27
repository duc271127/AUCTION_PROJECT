package com.team.backend.service.impl;

import com.team.backend.dto.ItemCreateDto;
import com.team.backend.dto.ItemDto;
import com.team.backend.entity.Item;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ItemServiceImpl - mở rộng để hỗ trợ thao tác theo seller:
 * - findBySellerId
 * - createForSeller
 * - updateForSeller
 * - deleteForSeller
 *
 * Vẫn giữ createItem(ItemCreateDto, UUID) và getItem(UUID) như trước.
 */
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) { this.itemRepository = itemRepository; }

    // -------------------------
    // Existing methods (kept)
    // -------------------------
    @Override
    @Transactional
    public Item createItem(ItemCreateDto dto, UUID sellerId) {
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
        item.setStartPrice(dto.startPrice);
        item.setSellerId(sellerId);
        item.setCreatedAt(Instant.now());

        return itemRepository.save(item);
    }

    @Override
    public Item getItem(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> new BusinessRuleException("Item not found: " + id));
    }

    // -------------------------
    // New methods for seller
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
        item.setStartPrice(dto.getStartingPrice());
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
            item.setStartPrice(dto.getStartingPrice());
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
    // Helpers: mapping
    // -------------------------
    private ItemDto toDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setSellerId(item.getSellerId());
        dto.setTitle(item.getName());
        dto.setDescription(item.getDescription());
        dto.setStartingPrice(item.getStartPrice());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }
}
