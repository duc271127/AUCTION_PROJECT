package com.auction.client.service;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.dto.response.ItemResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.session.SessionManager;

import java.util.List;
import java.util.UUID;

public class SellerItemApiService {

    private final ApiClient apiClient;

    // Backend nên dùng endpoint v2 cho frontend Seller Dashboard
    private static final String SELLER_ITEMS_ENDPOINT = "/seller/items/v2";

    public SellerItemApiService() {
        this.apiClient = new ApiClient();
    }

    public List<ItemResponse> getMyItems() throws ApiException {
        UUID sellerId = SessionManager.getUserId();

        if (sellerId == null) {
            throw new ApiException("Seller id is missing. Please login again.");
        }

        return apiClient.getList(
                SELLER_ITEMS_ENDPOINT + "?sellerId=" + sellerId,
                ItemResponse.class
        );
    }

    public ItemResponse createItem(CreateItemRequest request) throws ApiException {
        if (request == null) {
            throw new ApiException("Create item request is missing.");
        }

        if (request.getSellerId() == null) {
            request.setSellerId(SessionManager.getUserId());
        }

        if (request.getSellerId() == null) {
            throw new ApiException("Seller id is missing. Please login again.");
        }

        return apiClient.post(
                SELLER_ITEMS_ENDPOINT,
                request,
                ItemResponse.class
        );
    }

    public ItemResponse updateItem(UUID itemId, UpdateItemRequest request) throws ApiException {
        if (itemId == null) {
            throw new ApiException("Item id is missing.");
        }

        if (request == null) {
            throw new ApiException("Update item request is missing.");
        }

        if (request.getSellerId() == null) {
            request.setSellerId(SessionManager.getUserId());
        }

        if (request.getSellerId() == null) {
            throw new ApiException("Seller id is missing. Please login again.");
        }

        return apiClient.put(
                SELLER_ITEMS_ENDPOINT + "/" + itemId,
                request,
                ItemResponse.class
        );
    }

    public void deleteItem(UUID itemId) throws ApiException {
        if (itemId == null) {
            throw new ApiException("Item id is missing.");
        }

        UUID sellerId = SessionManager.getUserId();

        if (sellerId == null) {
            throw new ApiException("Seller id is missing. Please login again.");
        }

        apiClient.delete(
                SELLER_ITEMS_ENDPOINT + "/" + itemId + "?sellerId=" + sellerId
        );
    }
}