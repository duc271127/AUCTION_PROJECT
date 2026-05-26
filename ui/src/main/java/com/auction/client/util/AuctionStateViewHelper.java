package com.auction.client.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

public final class AuctionStateViewHelper {

    private AuctionStateViewHelper() {
    }

    public static String resolveDisplayState(String rawState, String startTime, String endTime) {
        String normalized = normalize(rawState);
        Instant now = Instant.now();
        Instant startInstant = parseInstant(startTime);
        Instant endInstant = parseInstant(endTime);

        if ("DELETED".equals(normalized) || "REJECTED".equals(normalized)) {
            return normalized;
        }

        if (isClosedFamily(normalized) || (endInstant != null && !now.isBefore(endInstant))) {
            return "CLOSED";
        }

        if (startInstant != null) {
            return now.isBefore(startInstant) ? "SCHEDULED" : "ACTIVE";
        }

        if (isLiveFamily(normalized)) {
            return "ACTIVE";
        }

        if (isScheduledFamily(normalized)) {
            return "SCHEDULED";
        }

        return normalized.isBlank() ? "UNKNOWN" : normalized;
    }

    public static boolean isActive(String rawState, String startTime, String endTime) {
        return "ACTIVE".equals(resolveDisplayState(rawState, startTime, endTime));
    }

    public static boolean isScheduled(String rawState, String startTime, String endTime) {
        return "SCHEDULED".equals(resolveDisplayState(rawState, startTime, endTime));
    }

    public static boolean isClosed(String rawState, String startTime, String endTime) {
        String displayState = resolveDisplayState(rawState, startTime, endTime);
        return "CLOSED".equals(displayState)
                || "DELETED".equals(displayState)
                || "REJECTED".equals(displayState);
    }

    private static boolean isLiveFamily(String normalized) {
        return "ACTIVE".equals(normalized)
                || "OPEN".equals(normalized)
                || "LIVE".equals(normalized);
    }

    private static boolean isScheduledFamily(String normalized) {
        return "SCHEDULED".equals(normalized)
                || "INCOMING".equals(normalized)
                || "PENDING".equals(normalized)
                || "DRAFT".equals(normalized);
    }

    private static boolean isClosedFamily(String normalized) {
        return "FINISHED".equals(normalized)
                || "CANCELLED".equals(normalized)
                || "CLOSED".equals(normalized)
                || "ENDED".equals(normalized);
    }

    private static String normalize(String rawState) {
        return rawState == null ? "" : rawState.trim().toUpperCase(Locale.ROOT);
    }

    private static Instant parseInstant(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(rawValue);
        } catch (Exception ignored) {
        }

        try {
            String normalized = rawValue.trim().replace(" ", "T");
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }
            return LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }
}
