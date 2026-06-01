package com.auction.client.service;

import com.auction.client.dto.response.PublicItemDetailResponse;
import javafx.scene.image.Image;

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

    public boolean isRemoteImagePath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return false;
        }

        return imagePath.startsWith("http://")
                || imagePath.startsWith("https://")
                || imagePath.startsWith("/uploads")
                || imagePath.startsWith("uploads/")
                || imagePath.startsWith("/api/uploads")
                || imagePath.startsWith("api/uploads");
    }

    public Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        if (isRemoteImagePath(imagePath)) {
            return new Image(toAbsoluteImageUrl(imagePath), true);
        }

        if (getClass().getResource(imagePath) == null) {
            return null;
        }

        return new Image(getClass().getResourceAsStream(imagePath));
    }
}
