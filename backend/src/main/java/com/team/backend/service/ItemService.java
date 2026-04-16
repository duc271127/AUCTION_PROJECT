package com.team.backend.service;

import com.team.backend.dto.ItemCreateDto;
import com.team.backend.entity.Item;

import java.util.UUID;

public interface ItemService {
    Item createItem(ItemCreateDto dto, UUID sellerId);
    Item getItem(UUID id);
}
