package com.auction.client.session;

import java.util.UUID;

public class SessionManager {
    private static String token;
    private static String username;
    private static String role;
    private static UUID userId;

    private SessionManager() {
    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        SessionManager.token = token;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        SessionManager.username = username;
    }

    public static String getRole() {
        return role;
    }

    public static void setRole(String role) {
        SessionManager.role = normalizeRole(role);
    }

    public static UUID getUserId() {
        return userId;
    }

    public static void setUserId(UUID userId) {
        SessionManager.userId = userId;
    }

    public static boolean isAuthenticated() {
        return role != null && !role.isBlank();
    }

    public static boolean hasRole(String expectedRole) {
        String normalizedCurrentRole = normalizeRole(role);
        String normalizedExpectedRole = normalizeRole(expectedRole);
        return !normalizedCurrentRole.isBlank()
                && normalizedCurrentRole.equals(normalizedExpectedRole);
    }

    public static String normalizeRole(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized;
    }

    public static void clear() {
        token = null;
        username = null;
        role = null;
        userId = null;
    }
}
