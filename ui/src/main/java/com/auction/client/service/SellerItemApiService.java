package com.auction.client.service;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.dto.response.ItemResponse;
import com.auction.client.exception.ApiException;

import java.util.List;

public class SellerItemApiService {

    private final ApiClient apiClient;

    // Đổi lại theo endpoint backend thật của nhóm bạn nếu khác
    private static final String SELLER_ITEMS_ENDPOINT = "/seller/items";

    public SellerItemApiService() {
        this.apiClient = new ApiClient();
    }

    public List<ItemResponse> getMyItems() throws ApiException {
        return apiClient.getList(SELLER_ITEMS_ENDPOINT, ItemResponse.class);
    }

    public ItemResponse createItem(CreateItemRequest request) throws ApiException {
        return apiClient.post(SELLER_ITEMS_ENDPOINT, request, ItemResponse.class);
    }

    public ItemResponse updateItem(Long itemId, UpdateItemRequest request) throws ApiException {
        return apiClient.put(SELLER_ITEMS_ENDPOINT + "/" + itemId, request, ItemResponse.class);
    }

    public void deleteItem(Long itemId) throws ApiException {
        apiClient.delete(SELLER_ITEMS_ENDPOINT + "/" + itemId);
    }
}