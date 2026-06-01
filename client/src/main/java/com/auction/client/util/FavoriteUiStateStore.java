package com.auction.client.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FavoriteUiStateStore {

    private static final Map<String, FavoriteState> STATES = new ConcurrentHashMap<>();

    private FavoriteUiStateStore() {
    }

    public static FavoriteState get(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return null;
        }
        return STATES.get(auctionId);
    }

    public static void put(String auctionId, boolean selected, int count) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }
        STATES.put(auctionId, new FavoriteState(selected, Math.max(0, count)));
    }

    public record FavoriteState(boolean selected, int count) {
    }
}
