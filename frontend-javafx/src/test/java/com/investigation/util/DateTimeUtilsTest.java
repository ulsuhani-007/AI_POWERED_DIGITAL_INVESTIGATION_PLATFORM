package com.investigation.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void testParseSafelyIso8601() {
        String iso = "2026-10-27T12:30:00Z";
        Instant parsed = DateTimeUtils.parseSafely(iso);
        assertNotNull(parsed);
        assertEquals("2026-10-27T12:30:00Z", parsed.toString());
    }

    @Test
    void testParseSafelyWithOffset() {
        String withOffset = "2026-10-27T14:30:00+02:00";
        Instant parsed = DateTimeUtils.parseSafely(withOffset);
        assertNotNull(parsed);
        assertEquals("2026-10-27T12:30:00Z", parsed.toString());
    }

    @Test
    void testParseSafelyFallback() {
        Instant fallback = Instant.ofEpochMilli(1000);
        Instant parsed = DateTimeUtils.parseSafely("invalid-timestamp", fallback);
        assertEquals(fallback, parsed);

        Instant parsedNull = DateTimeUtils.parseSafely(null, fallback);
        assertEquals(fallback, parsedNull);
    }

    @Test
    void testFormatUtc() {
        Instant instant = Instant.parse("2026-10-27T15:45:30Z");
        String formatted = DateTimeUtils.formatUtc(instant);
        assertTrue(formatted.contains("2026-10-27 15:45:30 UTC"));
    }
}
