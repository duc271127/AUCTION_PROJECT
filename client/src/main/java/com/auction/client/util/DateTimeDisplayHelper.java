package com.auction.client.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeDisplayHelper {

    private static final DateTimeFormatter MINUTE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private DateTimeDisplayHelper() {
    }

    public static String formatDateTime(String rawValue, String fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        try {
            return MINUTE_FORMAT.format(Instant.parse(rawValue));
        } catch (Exception ignored) {
        }

        try {
            String normalized = rawValue.trim().replace(" ", "T");
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }
            return LocalDateTime.parse(normalized)
                    .atZone(ZoneId.systemDefault())
                    .format(MINUTE_FORMAT);
        } catch (Exception ignored) {
        }

        return rawValue.length() >= 16
                ? rawValue.substring(0, 16).replace("T", " ")
                : rawValue;
    }
}
