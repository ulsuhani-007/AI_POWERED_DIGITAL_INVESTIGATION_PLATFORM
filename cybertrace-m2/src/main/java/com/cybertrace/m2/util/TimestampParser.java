package com.cybertrace.m2.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Robust multi-format timestamp parser supporting standard forensic log formats.
 */
public final class TimestampParser {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private TimestampParser() {
        // utility class
    }

    /**
     * Parses a raw timestamp string into a LocalDateTime.
     * Attempts strict pattern matching across common forensic timestamp patterns,
     * with fallback to ISO-8601 Instant parsing for UTC timestamps with 'Z' or offsets.
     *
     * @param rawTimestamp raw string from evidence file
     * @return parsed LocalDateTime
     * @throws IllegalArgumentException if the timestamp is null, blank, or cannot be parsed
     */
    public static LocalDateTime parse(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.trim().isEmpty()) {
            throw new IllegalArgumentException("Timestamp is missing or blank");
        }

        String trimmed = rawTimestamp.trim();

        // 1. Try common local patterns
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        // 2. Try Instant (e.g. 2026-10-26T10:30:00Z or with offset)
        try {
            Instant instant = Instant.parse(trimmed);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {
            // Fall through to error
        }

        throw new IllegalArgumentException(
                "Unparseable timestamp: '" + rawTimestamp + "'. Supported patterns include " +
                        "yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd HH:mm:ss, dd/MM/yyyy HH:mm:ss, and yyyy-MM-dd'T'HH:mm:ss.SSS"
        );
    }
}
