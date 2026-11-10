package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedEvidenceParserTest {

    private UnifiedEvidenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new UnifiedEvidenceParser();
    }

    @Test
    @DisplayName("Should detect unified schema headers")
    void testCanParse() {
        Set<String> headers = Set.of("id", "timestamp", "source", "event_type", "user", "ip_address", "device", "success", "bytes", "description");
        assertThat(parser.canParse(headers)).isTrue();
        assertThat(parser.getSupportedFileType()).isEqualTo(EvidenceFileType.UNIFIED);
    }

    @Test
    @DisplayName("Should parse 01_login_events.csv content with correct event types and login attempt descriptions")
    void testParseLoginEvents() {
        String csv = """
                id,timestamp,source,event_type,user,ip_address,device,success,bytes,description
                L001,2026-11-09 08:15:00,Login CSV,LOGIN,alice,10.10.10.21,WS-01,true,0,Normal morning login
                L004,2026-11-10 00:03:00,Login CSV,LOGIN_FAILURE,alice,10.10.10.21,WS-01,false,0,Failed login
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-1", "ev-1");
        assertThat(result.getErrors()).isEmpty();
        List<NormalizedEventDTO> events = result.getEvents();
        assertThat(events).hasSize(2);

        NormalizedEventDTO e1 = events.get(0);
        assertThat(e1.getEventId()).isEqualTo("L001");
        assertThat(e1.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 11, 9, 8, 15, 0));
        assertThat(e1.getEventType()).isEqualTo(EventType.LOGIN);
        assertThat(e1.getUser()).isEqualTo("alice");
        assertThat(e1.getIpAddress()).isEqualTo("10.10.10.21");
        assertThat(e1.getDevice()).isEqualTo("WS-01");
        assertThat(e1.getDescription()).contains("login attempt: SUCCESS");

        NormalizedEventDTO e2 = events.get(1);
        assertThat(e2.getEventId()).isEqualTo("L004");
        assertThat(e2.getEventType()).isEqualTo(EventType.LOGIN);
        assertThat(e2.getDescription()).contains("login attempt: FAILED");
    }

    @Test
    @DisplayName("Should parse 04_network_events.csv content with bytes and NETWORK_EVENT type")
    void testParseNetworkEvents() {
        String csv = """
                id,timestamp,source,event_type,user,ip_address,device,success,bytes,description
                N001,2026-11-09 11:00:00,Network CSV,NETWORK_TRANSFER,alice,10.10.10.21,WS-01,true,12000,Normal outbound transfer
                N003,2026-11-10 00:20:00,Network CSV,NETWORK_TRANSFER,alice,10.10.10.21,WS-01,true,95000000,Very large outbound transfer
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-1", "ev-4");
        assertThat(result.getErrors()).isEmpty();
        List<NormalizedEventDTO> events = result.getEvents();
        assertThat(events).hasSize(2);

        NormalizedEventDTO n1 = events.get(0);
        assertThat(n1.getEventId()).isEqualTo("N001");
        assertThat(n1.getEventType()).isEqualTo(EventType.NETWORK_EVENT);
        assertThat(n1.getBytes()).isEqualTo(12000L);

        NormalizedEventDTO n2 = events.get(1);
        assertThat(n2.getEventId()).isEqualTo("N003");
        assertThat(n2.getEventType()).isEqualTo(EventType.NETWORK_EVENT);
        assertThat(n2.getBytes()).isEqualTo(95000000L);
    }
}

