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

class NetworkEventsEvidenceParserTest {

    private NetworkEventsEvidenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new NetworkEventsEvidenceParser();
    }

    @Test
    @DisplayName("Should successfully parse valid network_events.csv entries")
    void testParseValidNetworkCsv() {
        String csv = """
                timestamp,sourceIp,destinationIp,port,bytes,device
                2026-10-26T10:30:15,192.168.1.10,198.51.100.42,443,15420,device1
                2026-10-26 10:31:00,10.0.0.5,8.8.8.8,53,128,device2
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-03", "ev-03");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getEvents()).hasSize(2);

        NormalizedEventDTO event1 = result.getEvents().get(0);
        assertThat(event1.getEventType()).isEqualTo(EventType.NETWORK_EVENT);
        assertThat(event1.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(event1.getDestinationIp()).isEqualTo("198.51.100.42");
        assertThat(event1.getPort()).isEqualTo(443);
        assertThat(event1.getBytes()).isEqualTo(15420L);
        assertThat(event1.getDevice()).isEqualTo("device1");
        assertThat(event1.getDescription()).isEqualTo("Network traffic from 192.168.1.10 to 198.51.100.42:443 (15420 bytes)");
        assertThat(event1.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 15));

        // Schema contract: network events do not have user, fileName, action, url
        assertThat(event1.getUser()).isNull();
        assertThat(event1.getFileName()).isNull();
        assertThat(event1.getAction()).isNull();
        assertThat(event1.getUrl()).isNull();
    }

    @Test
    @DisplayName("Should capture errors on non-numeric or invalid port and bytes")
    void testInvalidPortAndBytes() {
        String csv = """
                timestamp,sourceIp,destinationIp,port,bytes,device
                2026-10-26T10:30:15,10.0.0.1,10.0.0.2,abc,100,device1
                2026-10-26T10:30:16,10.0.0.1,10.0.0.2,70000,100,device1
                2026-10-26T10:30:17,10.0.0.1,10.0.0.2,80,-50,device1
                2026-10-26T10:30:18,10.0.0.1,10.0.0.2,80,not_a_number,device1
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-03", "ev-03");

        assertThat(result.getEvents()).isEmpty();
        assertThat(result.getErrors()).hasSize(4);
        assertThat(result.getErrors().get(0).getReason()).contains("Invalid port number: abc");
        assertThat(result.getErrors().get(1).getReason()).contains("Port out of range [0-65535]: 70000");
        assertThat(result.getErrors().get(2).getReason()).contains("Bytes cannot be negative: -50");
        assertThat(result.getErrors().get(3).getReason()).contains("Invalid bytes value: not_a_number");
    }

    @Test
    @DisplayName("Should verify supported types")
    void testSupportedTypes() {
        assertThat(parser.getSupportedFileType()).isEqualTo(EvidenceFileType.NETWORK);
        assertThat(parser.getSupportedEventType()).isEqualTo(EventType.NETWORK_EVENT);
    }
}
