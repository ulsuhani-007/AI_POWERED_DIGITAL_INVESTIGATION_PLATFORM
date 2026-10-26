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

class LoginEvidenceParserTest {

    private LoginEvidenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new LoginEvidenceParser();
    }

    @Test
    @DisplayName("Should successfully parse valid login.csv entries")
    void testParseValidLoginCsv() {
        String csv = """
                timestamp,user,ipAddress,device,status
                2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS
                2026-10-26 10:31:15,user2,10.0.0.5,device2,FAILED
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-123", "ev-456");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getEvents()).hasSize(2);
        assertThat(result.getTotalProcessedRows()).isEqualTo(2);

        NormalizedEventDTO event1 = result.getEvents().get(0);
        assertThat(event1.getEventId()).startsWith("urn:uuid:");
        assertThat(event1.getCaseId()).isEqualTo("case-123");
        assertThat(event1.getSourceEvidenceId()).isEqualTo("ev-456");
        assertThat(event1.getEventType()).isEqualTo(EventType.LOGIN);
        assertThat(event1.getUser()).isEqualTo("user1");
        assertThat(event1.getDevice()).isEqualTo("device1");
        assertThat(event1.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(event1.getDescription()).isEqualTo("User user1 login attempt: SUCCESS");
        assertThat(event1.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 0));

        // Schema contract: non-applicable fields must be null
        assertThat(event1.getDestinationIp()).isNull();
        assertThat(event1.getFileName()).isNull();
        assertThat(event1.getAction()).isNull();
        assertThat(event1.getUrl()).isNull();
        assertThat(event1.getPort()).isNull();
        assertThat(event1.getBytes()).isNull();
    }

    @Test
    @DisplayName("Should handle case-insensitive headers and whitespace trimming")
    void testCaseInsensitiveAndTrimmedHeaders() {
        String csv = """
                 TIMESTAMP , USER , IPADDRESS , DEVICE , STATUS\s
                2026-10-26T10:30:00,  user_alpha  , 10.1.1.1 , ws-01 , SUCCESS
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-123", "ev-456");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getEvents()).hasSize(1);
        NormalizedEventDTO event = result.getEvents().get(0);
        assertThat(event.getUser()).isEqualTo("user_alpha");
        assertThat(event.getIpAddress()).isEqualTo("10.1.1.1");
        assertThat(event.getDevice()).isEqualTo("ws-01");
        assertThat(event.getDescription()).isEqualTo("User user_alpha login attempt: SUCCESS");
    }

    @Test
    @DisplayName("Should capture unparseable date, blank row, and missing column in error audit")
    void testCorruptedRowsHandling() {
        String csv = """
                timestamp,user,ipAddress,device,status
                2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS
                26-99-2026 10:00:00,user2,10.0.0.5,device2,FAILED
                
                ,user3,10.0.0.6,device3,SUCCESS
                2026-10-26T10:35:00,user4,192.168.1.20,device4,
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-123", "ev-456");

        assertThat(result.getEvents()).hasSize(1);
        assertThat(result.getEvents().get(0).getUser()).isEqualTo("user1");

        assertThat(result.getErrors()).hasSize(4);

        // Line 3: Unparseable date
        assertThat(result.getErrors().get(0).getLineNumber()).isEqualTo(3);
        assertThat(result.getErrors().get(0).getReason()).contains("Unparseable date: 26-99-2026");

        // Line 4: Blank row
        assertThat(result.getErrors().get(1).getLineNumber()).isEqualTo(4);
        assertThat(result.getErrors().get(1).getReason()).isEqualTo("Blank row");

        // Line 5: Missing required column timestamp
        assertThat(result.getErrors().get(2).getLineNumber()).isEqualTo(5);
        assertThat(result.getErrors().get(2).getReason()).contains("Missing required column: timestamp");

        // Line 6: Missing required column status
        assertThat(result.getErrors().get(3).getLineNumber()).isEqualTo(6);
        assertThat(result.getErrors().get(3).getReason()).contains("Missing required column: status");
    }

    @Test
    @DisplayName("Should verify supported file and event types")
    void testSupportedTypes() {
        assertThat(parser.getSupportedFileType()).isEqualTo(EvidenceFileType.LOGIN);
        assertThat(parser.getSupportedEventType()).isEqualTo(EventType.LOGIN);
    }
}
