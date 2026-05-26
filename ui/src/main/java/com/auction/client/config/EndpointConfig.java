package com.auction.client.config;

public final class EndpointConfig {

    private static final String DEFAULT_HTTP_BASE_URL = "http://localhost:8081";
    private static final String DEFAULT_WS_URL = "ws://localhost:8081/ws/websocket";

    private EndpointConfig() {
    }

    public static String getHttpBaseUrl() {
        String configured = firstNonBlank(
                System.getProperty("auction.api.baseUrl"),
                System.getenv("AUCTION_API_BASE_URL")
        );

        return configured == null ? DEFAULT_HTTP_BASE_URL : configured;
    }

    public static String getWebSocketUrl() {
        String configured = firstNonBlank(
                System.getProperty("auction.ws.url"),
                System.getenv("AUCTION_WS_URL")
        );

        if (configured != null) {
            return configured;
        }

        String httpBaseUrl = getHttpBaseUrl();

        if (httpBaseUrl.startsWith("https://")) {
            return "wss://" + httpBaseUrl.substring("https://".length()) + "/ws/websocket";
        }

        if (httpBaseUrl.startsWith("http://")) {
            return "ws://" + httpBaseUrl.substring("http://".length()) + "/ws/websocket";
        }

        return DEFAULT_WS_URL;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }
}
