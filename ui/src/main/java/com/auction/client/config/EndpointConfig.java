package com.auction.client.config;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public final class EndpointConfig {

    private static final String LOCAL_HTTP_BASE_URL = "http://127.0.0.1:8081";
    private static final String DEFAULT_HTTP_BASE_URL = "http://lungs-decree.with.playit.plus:1125";
    private static final String DEFAULT_WS_URL = "ws://lungs-decree.with.playit.plus:1125/ws/websocket";
    private static final String HTTP_BASE_URL_KEY = "auction.api.baseUrl";
    private static final String WS_URL_KEY = "auction.ws.url";
    private static final String CLASSPATH_CONFIG = "auction-client.properties";
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(800);
    private static volatile String resolvedHttpBaseUrl;

    private EndpointConfig() {
    }

    public static String getHttpBaseUrl() {
        String explicitOverride = firstNonBlank(
                System.getProperty(HTTP_BASE_URL_KEY),
                System.getenv("AUCTION_API_BASE_URL")
        );

        if (explicitOverride != null) {
            return normalizeHttpBaseUrl(explicitOverride);
        }

        String cached = resolvedHttpBaseUrl;
        if (cached != null) {
            if (LOCAL_HTTP_BASE_URL.equals(cached) || !isReachable(LOCAL_HTTP_BASE_URL)) {
                return cached;
            }

            String normalizedLocal = normalizeHttpBaseUrl(LOCAL_HTTP_BASE_URL);
            resolvedHttpBaseUrl = normalizedLocal;
            return normalizedLocal;
        }

        String configuredDefault = firstNonBlank(
                readClasspathProperty(HTTP_BASE_URL_KEY),
                DEFAULT_HTTP_BASE_URL
        );

        String preferred = isReachable(LOCAL_HTTP_BASE_URL)
                ? LOCAL_HTTP_BASE_URL
                : configuredDefault;
        String normalized = normalizeHttpBaseUrl(preferred);
        resolvedHttpBaseUrl = normalized;
        return normalized;
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

    private static boolean isReachable(String httpBaseUrl) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeHttpBaseUrl(httpBaseUrl) + "/api/auctions?page=0&size=1"))
                    .timeout(CONNECT_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
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
