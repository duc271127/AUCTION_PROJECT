package com.team.backend.util;

import com.team.backend.entity.Auction;
import com.team.backend.entity.Item;

public final class AuctionImageResolver {

    private AuctionImageResolver() {
    }

    public static String resolvePrimaryImage(Auction auction) {
        if (auction == null) {
            return null;
        }

        String directImage = normalize(auction.getImageUrl());
        if (directImage != null) {
            return directImage;
        }

        Item item = auction.getItem();
        if (item == null) {
            return null;
        }

        String imagePath = normalize(item.getImagePath());
        if (imagePath != null) {
            return imagePath;
        }

        if (item.getImageUrls() == null || item.getImageUrls().isEmpty()) {
            return null;
        }

        for (String imageUrl : item.getImageUrls()) {
            String normalized = normalize(imageUrl);
            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
