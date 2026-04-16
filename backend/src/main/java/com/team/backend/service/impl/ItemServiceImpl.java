package com.team.backend.service.impl;

import com.team.backend.dto.ItemCreateDto;
import com.team.backend.entity.Item;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.ItemRepository;
import com.team.backend.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Item creation rules:
 * - name not blank
 * - startPrice > 0
 * - sellerId must be provided (controller should ensure caller is seller)
 */
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) { this.itemRepository = itemRepository; }

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

        return itemRepository.save(item);
    }

    @Override
    public Item getItem(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> new BusinessRuleException("Item not found: " + id));
    }
}
