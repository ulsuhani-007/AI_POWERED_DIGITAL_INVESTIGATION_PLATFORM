package com.cybertrace.m2.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimestampParserTest {

    @Test
    @DisplayName("Should parse standard ISO-8601 without offset: yyyy-MM-dd'T'HH:mm:ss")
    void testParseIsoStandard() {
        LocalDateTime result = TimestampParser.parse("2026-10-26T10:30:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 0));
    }

    @Test
    @DisplayName("Should parse space separated format: yyyy-MM-dd HH:mm:ss")
    void testParseSpaceSeparated() {
        LocalDateTime result = TimestampParser.parse("2026-10-26 10:31:15");
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 31, 15));
    }

    @Test
    @DisplayName("Should parse European format: dd/MM/yyyy HH:mm:ss")
    void testParseEuropeanFormat() {
        LocalDateTime result = TimestampParser.parse("26/10/2026 10:32:00");
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 32, 0));
    }

    @Test
    @DisplayName("Should parse ISO with fractional milliseconds: yyyy-MM-dd'T'HH:mm:ss.SSS")
    void testParseIsoWithMillis() {
        LocalDateTime result = TimestampParser.parse("2026-10-26T10:35:00.123");
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 35, 0, 123000000));
    }

    @Test
    @DisplayName("Should parse timestamp with leading/trailing whitespaces")
    void testParseWithWhitespace() {
        LocalDateTime result = TimestampParser.parse("   2026-10-26T10:30:00   ");
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"26-99-2026", "invalid-date", "2026/99/99 99:99:99", "10-26-2026"})
    @DisplayName("Should throw IllegalArgumentException on unparseable format")
    void testUnparseableFormats(String badDate) {
        assertThatThrownBy(() -> TimestampParser.parse(badDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unparseable timestamp");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should throw IllegalArgumentException on blank or empty timestamp")
    void testBlankTimestamp(String emptyDate) {
        assertThatThrownBy(() -> TimestampParser.parse(emptyDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamp is missing or blank");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on null timestamp")
    void testNullTimestamp() {
        assertThatThrownBy(() -> TimestampParser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamp is missing or blank");
    }
}
