package com.auction.client.service;

import com.auction.client.dto.request.CreateAuctionRequest;
import com.auction.client.dto.response.AdminStatsResponse;
import com.auction.client.dto.response.AdminWalletActivityResponse;
import com.auction.client.dto.response.AdminNotificationResponse;
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

    public AdminStatsResponse getStats() {
        try {
            String responseBody = apiClient.get("/admin/stats");
            return objectMapper.readValue(responseBody, AdminStatsResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load admin stats failed: " + e.getMessage(), e);
        }
    }

    public List<AdminWalletActivityResponse> getRecentWalletActivity(int limit) {
        try {
            String responseBody = apiClient.get("/admin/wallet-activity/recent?limit=" + limit);
            return objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<AdminWalletActivityResponse>>() {}
            );
        } catch (Exception e) {
            throw new ApiException("Load wallet activity failed: " + e.getMessage(), e);
        }
    }

    public List<AdminNotificationResponse> getRecentNotifications(int limit) {
        try {
            String responseBody = apiClient.get("/admin/notifications/recent?limit=" + limit);
            return objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<AdminNotificationResponse>>() {}
            );
        } catch (Exception e) {
            throw new ApiException("Load notifications failed: " + e.getMessage(), e);
        }
    }

    public AdminApprovalItem rejectItem(String itemId) {
        try {
            String responseBody = apiClient.post("/admin/items/" + itemId + "/reject", "");
            return objectMapper.readValue(responseBody, AdminApprovalItem.class);
        } catch (Exception e) {
            throw new ApiException("Reject item failed: " + e.getMessage(), e);
        }
    }

    public AuctionListResponse approveAndCreateAuction(String itemId, CreateAuctionRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            String responseBody = apiClient.post("/admin/items/" + itemId + "/approve-and-create-auction", jsonBody);
            return objectMapper.readValue(responseBody, AuctionListResponse.class);
        } catch (Exception e) {
            if (shouldFallbackApproveAndCreate(e)) {
                approveItem(itemId);
                return createAuctionForItem(itemId, request);
            }
            throw new ApiException("Approve and create auction failed: " + e.getMessage(), e);
        }
    }

    public void deleteItem(String itemId) {
        apiClient.delete("/admin/items/" + itemId);
    }

    public void acceptAuction(String auctionId) {
        try {
            apiClient.post("/admin/auctions/" + auctionId + "/accept", "");
        } catch (Exception e) {
            throw new ApiException("Accept auction failed: " + e.getMessage(), e);
        }
    }

    public void rejectAuction(String auctionId) {
        try {
            apiClient.post("/admin/auctions/" + auctionId + "/reject", "");
        } catch (Exception e) {
            throw new ApiException("Reject auction failed: " + e.getMessage(), e);
        }
    }

    public void deleteAuction(String auctionId) {
        try {
            apiClient.delete("/admin/auctions/" + auctionId);
        } catch (Exception e) {
            throw new ApiException("Delete auction failed: " + e.getMessage(), e);
        }
    }

    private boolean shouldFallbackApproveAndCreate(Exception exception) {
        String message = exception == null ? "" : String.valueOf(exception.getMessage());
        String normalized = message.toLowerCase();
        return normalized.contains("404")
                || normalized.contains("no static resource")
                || normalized.contains("approve-and-create-auction");
    }

}
