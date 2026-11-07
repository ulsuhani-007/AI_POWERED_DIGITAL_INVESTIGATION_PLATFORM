package com.investigation.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility functions for parsing ISO-8601 strings safely and formatting timestamps.
 */
public final class DateTimeUtils {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter TIME_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private DateTimeUtils() {}

    /**
     * Safely parses an ISO-8601 or common timestamp string into an Instant.
     * Returns fallback if invalid or null.
     */
    public static Instant parseSafely(String text, Instant fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String trimmed = text.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException e1) {
            try {
                return OffsetDateTime.parse(trimmed).toInstant();
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
                } catch (DateTimeParseException e3) {
                    try {
                        if (trimmed.contains(" ")) {
                            return LocalDateTime.parse(trimmed.replace(" ", "T")).toInstant(ZoneOffset.UTC);
                        }
                    } catch (DateTimeParseException ignored) {}
                    try {
                        long epoch = Long.parseLong(trimmed);
                        if (epoch > 100000000000L) {
                            return Instant.ofEpochMilli(epoch);
                        } else {
                            return Instant.ofEpochSecond(epoch);
                        }
                    } catch (NumberFormatException ignored) {}
                    return fallback;
                }
            }
        }
    }

    public static Instant parseSafely(String text) {
        return parseSafely(text, Instant.now());
    }

    public static String formatUtc(Instant instant) {
        if (instant == null) return "N/A";
        return DISPLAY_FORMATTER.format(instant);
    }

    public static String formatShortDate(Instant instant) {
        if (instant == null) return "N/A";
        return SHORT_DATE_FORMATTER.format(instant);
    }

    public static String formatTimeOnly(Instant instant) {
        if (instant == null) return "N/A";
        return TIME_ONLY_FORMATTER.format(instant);
    }

    public static String getRelativeTime(Instant instant) {
        if (instant == null) return "N/A";
        long seconds = Instant.now().getEpochSecond() - instant.getEpochSecond();
        if (seconds < 0) return "Just now";
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }
}
