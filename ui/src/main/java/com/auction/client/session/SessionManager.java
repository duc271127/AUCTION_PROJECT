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
        SessionManager.role = role;
    }
    public static UUID getUserId() {
        return userId;
    }
    public static void setUserId(UUID userId) {
        SessionManager.userId = userId;
    }


    public static void clear() {
        token = null;
        username = null;
        role = null;
        userId = null;
    }
}