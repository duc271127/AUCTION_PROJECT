package com.auction.client.service;

import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class FavoriteApiService {
    private final ApiClient apiClient = new ApiClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AuctionDetailResponse> getFavorites() {
        try {
            String responseBody = apiClient.get("/api/favorites");
            return objectMapper.readValue(responseBody, new TypeReference<List<AuctionDetailResponse>>() {});
        } catch (Exception e) {
            throw new ApiException("Load favorites failed: " + e.getMessage(), e);
        }
    }

    public void addFavorite(String auctionId) {
        try {
            apiClient.post("/api/favorites/" + auctionId, "");
        } catch (Exception e) {
            throw new ApiException("Add favorite failed: " + e.getMessage(), e);
        }
    }

    public void removeFavorite(String auctionId) {
        try {
            apiClient.delete("/api/favorites/" + auctionId);
        } catch (Exception e) {
            throw new ApiException("Remove favorite failed: " + e.getMessage(), e);
        }
    }
}
