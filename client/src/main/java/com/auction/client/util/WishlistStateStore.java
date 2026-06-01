package com.auction.client.util;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WishlistStateStore {

    private static final Set<String> FAVORITE_IDS = ConcurrentHashMap.newKeySet();

    private WishlistStateStore() {
    }

    public static void replaceAll(Collection<String> auctionIds) {
        FAVORITE_IDS.clear();
        if (auctionIds == null) {
            return;
        }
        for (String auctionId : auctionIds) {
            add(auctionId);
        }
    }

    public static void add(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }
        FAVORITE_IDS.add(auctionId);
    }

    public static void remove(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }
        FAVORITE_IDS.remove(auctionId);
    }

    public static boolean contains(String auctionId) {
        return auctionId != null && !auctionId.isBlank() && FAVORITE_IDS.contains(auctionId);
    }

    public static int count() {
        return FAVORITE_IDS.size();
    }
}
