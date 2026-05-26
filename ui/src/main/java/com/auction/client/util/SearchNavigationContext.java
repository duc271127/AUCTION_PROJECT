package com.auction.client.util;

public final class SearchNavigationContext {

    private static String pendingQuery;

    private SearchNavigationContext() {
    }

    public static void setPendingQuery(String query) {
        pendingQuery = normalize(query);
    }

    public static String consumePendingQuery() {
        String value = pendingQuery;
        pendingQuery = null;
        return value;
    }

    private static String normalize(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
