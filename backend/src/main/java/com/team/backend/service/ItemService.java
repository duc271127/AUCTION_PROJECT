package com.team.backend.service;

import com.team.backend.dto.ItemCreateDto;
import com.team.backend.dto.ItemDto;
import com.team.backend.entity.Item;

import java.util.UUID;
import java.util.List;

public interface ItemService {
    Item createItem(ItemCreateDto dto, UUID sellerId);
    Item getItem(UUID id);
    List<ItemDto> findBySellerId(UUID sellerId);
    ItemDto createForSeller(ItemDto dto);
    ItemDto updateForSeller(UUID itemId, UUID sellerId, ItemDto dto);
    void deleteForSeller(UUID itemId, UUID sellerId);
}
