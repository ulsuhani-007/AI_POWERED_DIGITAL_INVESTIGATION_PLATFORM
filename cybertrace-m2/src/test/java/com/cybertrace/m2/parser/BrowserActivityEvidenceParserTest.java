package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserActivityEvidenceParserTest {

    private BrowserActivityEvidenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new BrowserActivityEvidenceParser();
    }

    @Test
    @DisplayName("Should successfully parse valid browser_activity.csv entries")
    void testParseValidBrowserCsv() {
        String csv = """
                timestamp,user,url,device
                2026-10-26T10:33:00,user1,https://portal.company.com,device1
                2026-10-26 10:34:00,user2,https://external-site.org,device2
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-01", "ev-01");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getEvents()).hasSize(2);

        NormalizedEventDTO event1 = result.getEvents().get(0);
        assertThat(event1.getEventType()).isEqualTo(EventType.BROWSER_ACTIVITY);
        assertThat(event1.getUser()).isEqualTo("user1");
        assertThat(event1.getUrl()).isEqualTo("https://portal.company.com");
        assertThat(event1.getDevice()).isEqualTo("device1");
        assertThat(event1.getDescription()).isEqualTo("User user1 visited URL: https://portal.company.com");
        assertThat(event1.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 33, 0));

        // Strict schema contract verification
        assertThat(event1.getIpAddress()).isNull();
        assertThat(event1.getDestinationIp()).isNull();
        assertThat(event1.getFileName()).isNull();
        assertThat(event1.getAction()).isNull();
        assertThat(event1.getPort()).isNull();
        assertThat(event1.getBytes()).isNull();
    }

    @Test
    @DisplayName("Should capture errors on missing URL or corrupt date")
    void testCorruptRows() {
        String csv = """
                timestamp,user,url,device
                2026-10-26T10:33:00,user1,,device1
                invalid-date,user2,https://test.com,device2
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-01", "ev-01");

        assertThat(result.getEvents()).isEmpty();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0).getReason()).contains("Missing required column: url");
        assertThat(result.getErrors().get(1).getReason()).contains("Unparseable date: invalid-date");
    }

    @Test
    @DisplayName("Should verify supported types")
    void testSupportedTypes() {
        assertThat(parser.getSupportedFileType()).isEqualTo(EvidenceFileType.BROWSER);
        assertThat(parser.getSupportedEventType()).isEqualTo(EventType.BROWSER_ACTIVITY);
    }
}
