package com.auction.client.service;

import com.auction.client.dto.response.PublicItemDetailResponse;

public class ItemApiService {
    private final ApiClient apiClient = new ApiClient();

    public PublicItemDetailResponse getPublicItemDetail(String itemId) {
        return new com.google.gson.Gson().fromJson(
                apiClient.get("/api/items/" + itemId),
                PublicItemDetailResponse.class
        );
    }

    public String toAbsoluteImageUrl(String imagePath) {
        return apiClient.toAbsoluteUrl(imagePath);
    }
}
