package com.auction.client.config;

import java.io.InputStream;
import java.util.Properties;

public final class EndpointConfig {

    private static final String DEFAULT_HTTP_BASE_URL = "http://lungs-decree.with.playit.plus:1125";
    private static final String DEFAULT_WS_URL = "ws://lungs-decree.with.playit.plus:1125/ws/websocket";
    private static final String HTTP_BASE_URL_KEY = "auction.api.baseUrl";
    private static final String WS_URL_KEY = "auction.ws.url";
    private static final String CLASSPATH_CONFIG = "auction-client.properties";

    private EndpointConfig() {
    }

    public static String getHttpBaseUrl() {
        String configured = firstNonBlank(
                System.getProperty(HTTP_BASE_URL_KEY),
                System.getenv("AUCTION_API_BASE_URL"),
                readClasspathProperty(HTTP_BASE_URL_KEY)
        );

        return normalizeHttpBaseUrl(configured == null ? DEFAULT_HTTP_BASE_URL : configured);
    }

    public static String getWebSocketUrl() {
        String configured = firstNonBlank(
                System.getProperty(WS_URL_KEY),
                System.getenv("AUCTION_WS_URL"),
                readClasspathProperty(WS_URL_KEY)
        );

        if (configured != null) {
            return normalizeWebSocketUrl(configured);
        }

        return buildWebSocketUrl(getHttpBaseUrl());
    }

    public static String normalizeHttpBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Server URL cannot be empty.");
        }

        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static String buildWebSocketUrl(String httpBaseUrl) {
        if (httpBaseUrl.startsWith("https://")) {
            return "wss://" + httpBaseUrl.substring("https://".length()) + "/ws/websocket";
        }

        if (httpBaseUrl.startsWith("http://")) {
            return "ws://" + httpBaseUrl.substring("http://".length()) + "/ws/websocket";
        }

        return DEFAULT_WS_URL;
    }

    private static String normalizeWebSocketUrl(String value) {
        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) {
            return DEFAULT_WS_URL;
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private static String readClasspathProperty(String key) {
        Properties properties = new Properties();

        try (InputStream inputStream = EndpointConfig.class
                .getClassLoader()
                .getResourceAsStream(CLASSPATH_CONFIG)) {
            if (inputStream == null) {
                return null;
            }

            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (Exception e) {
            return null;
        }
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
