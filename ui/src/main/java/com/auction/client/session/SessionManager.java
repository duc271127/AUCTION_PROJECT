package com.auction.client.session;

public class SessionManager {
    private static String token;
    private static String username;
    private static String role;

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

    public static void clear() {
        token = null;
        username = null;
        role = null;
    }
}