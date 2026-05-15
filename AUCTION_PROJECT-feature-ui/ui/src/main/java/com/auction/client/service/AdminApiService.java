package com.auction.client.service;

import com.auction.client.dto.request.CreateAuctionRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.model.AdminApprovalItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class AdminApiService {
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    public AdminApiService() {
        this.apiClient = new ApiClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<AdminApprovalItem> getPendingItems() {
        try {
            String responseBody = apiClient.get("/admin/items/pending");
            return objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<AdminApprovalItem>>() {}
            );
        } catch (Exception e) {
            throw new ApiException("Load pending items failed: " + e.getMessage(), e);
        }
    }

    public AdminApprovalItem approveItem(String itemId) {
        try {
            String responseBody = apiClient.post("/admin/items/" + itemId + "/approve", "");
            return objectMapper.readValue(responseBody, AdminApprovalItem.class);
        } catch (Exception e) {
            throw new ApiException("Approve item failed: " + e.getMessage(), e);
        }
    }

    public AuctionListResponse createAuctionForItem(String itemId, CreateAuctionRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            String responseBody = apiClient.post("/admin/items/" + itemId + "/create-auction", jsonBody);
            return objectMapper.readValue(responseBody, AuctionListResponse.class);
        } catch (Exception e) {
            throw new ApiException("Create auction failed: " + e.getMessage(), e);
        }
    }
}