package com.auction.server.service;

import com.auction.server.dto.ItemCreateDto;
import com.auction.server.dto.ItemCreateRequest;
import com.auction.server.dto.ItemDto;
import com.auction.server.dto.ItemResponse;
import com.auction.server.dto.PublicItemDetailDto;
import com.auction.server.entity.Item;

import java.util.List;
import java.util.UUID;

/**
 * ItemService - interface mở rộng để hỗ trợ cả API nội bộ (entity/Item, ItemDto)
 * và contract trả về cho frontend (ItemCreateRequest, ItemResponse).
 *
 * Giữ lại các method cũ để tương thích, thêm method mới cho frontend.
 */
public interface ItemService {

    // ----- existing / legacy API (kept) -----
    Item createItem(ItemCreateDto dto, UUID sellerId);
    Item getItem(UUID id);
    PublicItemDetailDto getPublicItemDetail(UUID id);

    List<ItemDto> findBySellerId(UUID sellerId);
    ItemDto createForSeller(ItemDto dto);
    ItemDto updateForSeller(UUID itemId, UUID sellerId, ItemDto dto);
    void deleteForSeller(UUID itemId, UUID sellerId);

    // ----- new API for frontend contract (ItemCreateRequest / ItemResponse) -----

    /**
     * Trả về danh sách ItemResponse (format phù hợp frontend) của seller,
     * sắp xếp theo createdAt desc.
     */
    List<ItemResponse> findResponsesBySellerId(UUID sellerId);
    List<ItemResponse> findRecentResponsesBySellerId(UUID sellerId, int limit);

    /**
     * Tạo item cho seller dựa trên request từ frontend, trả về ItemResponse.
     */
    ItemResponse createForSeller(UUID sellerId, ItemCreateRequest request);

    /**
     * Cập nhật item cho seller dựa trên request từ frontend, trả về ItemResponse.
     */
    ItemResponse updateForSeller(UUID itemId, UUID sellerId, ItemCreateRequest request);

    /**
     * Xóa item cho seller (giữ nguyên hành vi).
     */
    void deleteForSellerResponse(UUID itemId, UUID sellerId);
}

