package com.auction.client.ui;

public record AuctionCardData(
        String id,
        String eyebrow,
        String title,
        String priceText,
        String statusText,
        String metaText,
        String imagePath,
        String badgeText,
        String actionText,
        String favoriteCountText
) {
}
