package com.cybertrace.m3.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizedEventDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("1. Deserializes exact M2 JSON structure for a LOGIN event")
    void testDeserializeActualM2LoginJson() throws Exception {
        String json = """
                {
                  "event_id": "urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479",
                  "case_id": "case-123",
                  "timestamp": "2026-10-26T10:30:00",
                  "event_type": "LOGIN",
                  "user": "user1",
                  "device": "device1",
                  "ip_address": "192.168.1.10",
                  "destination_ip": null,
                  "file_name": null,
                  "action": null,
                  "url": null,
                  "port": null,
                  "bytes": null,
                  "source_evidence_id": "evidence-123",
                  "description": "User user1 login attempt: SUCCESS"
                }
                """;

        NormalizedEventDTO dto = objectMapper.readValue(json, NormalizedEventDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getEventId()).isEqualTo("urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479");
        assertThat(dto.getCaseId()).isEqualTo("case-123");
        assertThat(dto.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 0));
        assertThat(dto.getEventType()).isEqualTo("LOGIN");
        assertThat(dto.getUser()).isEqualTo("user1");
        assertThat(dto.getDevice()).isEqualTo("device1");
        assertThat(dto.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(dto.getDestinationIp()).isNull();
        assertThat(dto.getFileName()).isNull();
        assertThat(dto.getAction()).isNull();
        assertThat(dto.getUrl()).isNull();
        assertThat(dto.getPort()).isNull();
        assertThat(dto.getBytes()).isNull();
        assertThat(dto.getSourceEvidenceId()).isEqualTo("evidence-123");
        assertThat(dto.getDescription()).isEqualTo("User user1 login attempt: SUCCESS");
    }

    @Test
    @DisplayName("2. Deserializes M2 NETWORK_EVENT with bytes, port, and destination_ip")
    void testDeserializeActualM2NetworkEventJson() throws Exception {
        String json = """
                {
                  "event_id": "urn:uuid:a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                  "case_id": "case-999",
                  "timestamp": "2026-10-26T10:39:00",
                  "event_type": "NETWORK_EVENT",
                  "user": null,
                  "device": "device1",
                  "ip_address": "192.168.1.10",
                  "destination_ip": "185.220.101.5",
                  "file_name": null,
                  "action": null,
                  "url": null,
                  "port": 8080,
                  "bytes": 4500000,
                  "source_evidence_id": "ev-net-01",
                  "description": "Network traffic from 192.168.1.10 to 185.220.101.5:8080 (4500000 bytes)"
                }
                """;

        NormalizedEventDTO dto = objectMapper.readValue(json, NormalizedEventDTO.class);

        assertThat(dto.getEventType()).isEqualTo("NETWORK_EVENT");
        assertThat(dto.getUser()).isNull();
        assertThat(dto.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(dto.getDestinationIp()).isEqualTo("185.220.101.5");
        assertThat(dto.getPort()).isEqualTo(8080);
        assertThat(dto.getBytes()).isEqualTo(4500000L);
    }

    @Test
    @DisplayName("3. Deserializes M2 FILE_ACCESS event with fileName and action")
    void testDeserializeActualM2FileAccessJson() throws Exception {
        String json = """
                {
                  "event_id": "urn:uuid:bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
                  "case_id": "case-001",
                  "timestamp": "2026-10-26T10:35:10",
                  "event_type": "FILE_ACCESS",
                  "user": "user2",
                  "device": "device2",
                  "ip_address": null,
                  "destination_ip": null,
                  "file_name": "D:\\\\Database\\\\dump.sql",
                  "action": "EXPORT",
                  "url": null,
                  "port": null,
                  "bytes": null,
                  "source_evidence_id": "ev-file-01",
                  "description": "User user2 executed action EXPORT on file D:\\\\Database\\\\dump.sql"
                }
                """;

        NormalizedEventDTO dto = objectMapper.readValue(json, NormalizedEventDTO.class);

        assertThat(dto.getEventType()).isEqualTo("FILE_ACCESS");
        assertThat(dto.getFileName()).isEqualTo("D:\\Database\\dump.sql");
        assertThat(dto.getAction()).isEqualTo("EXPORT");
    }

    @Test
    @DisplayName("4. Deserializes M2 BROWSER_ACTIVITY event with url")
    void testDeserializeActualM2BrowserJson() throws Exception {
        String json = """
                {
                  "event_id": "urn:uuid:cccccccc-dddd-eeee-ffff-aaaaaaaaaaaa",
                  "case_id": "case-001",
                  "timestamp": "2026-10-26T10:37:00",
                  "event_type": "BROWSER_ACTIVITY",
                  "user": "user2",
                  "device": "device2",
                  "ip_address": null,
                  "destination_ip": null,
                  "file_name": null,
                  "action": null,
                  "url": "https://malicious-c2-beacon.ru/gate.php",
                  "port": null,
                  "bytes": null,
                  "source_evidence_id": "ev-browser-01",
                  "description": "User user2 visited URL: https://malicious-c2-beacon.ru/gate.php"
                }
                """;

        NormalizedEventDTO dto = objectMapper.readValue(json, NormalizedEventDTO.class);

        assertThat(dto.getEventType()).isEqualTo("BROWSER_ACTIVITY");
        assertThat(dto.getUrl()).isEqualTo("https://malicious-c2-beacon.ru/gate.php");
    }

    @Test
    @DisplayName("5. Verifies handling of nullable optional fields")
    void testNullableOptionalFields() throws Exception {
        NormalizedEventDTO dto = NormalizedEventDTO.builder()
                .eventId("urn:uuid:test-id")
                .caseId("case-min")
                .timestamp(LocalDateTime.of(2026, 10, 26, 12, 0, 0))
                .eventType("CUSTOM")
                .sourceEvidenceId("src-1")
                .build();

        assertThat(dto.getUser()).isNull();
        assertThat(dto.getDevice()).isNull();
        assertThat(dto.getIpAddress()).isNull();
        assertThat(dto.getDestinationIp()).isNull();
        assertThat(dto.getFileName()).isNull();
        assertThat(dto.getAction()).isNull();
        assertThat(dto.getUrl()).isNull();
        assertThat(dto.getPort()).isNull();
        assertThat(dto.getBytes()).isNull();
        assertThat(dto.getDescription()).isNull();

        // Roundtrip serialization
        String json = objectMapper.writeValueAsString(dto);
        NormalizedEventDTO deserialized = objectMapper.readValue(json, NormalizedEventDTO.class);
        assertThat(deserialized).isEqualTo(dto);
    }

    @Test
    @DisplayName("6. Verifies exact snake_case JSON field names on serialization")
    void testExactJsonFieldNames() throws Exception {
        NormalizedEventDTO dto = NormalizedEventDTO.builder()
                .eventId("urn:uuid:test-id")
                .caseId("case-123")
                .timestamp(LocalDateTime.of(2026, 10, 26, 8, 30, 0))
                .eventType("LOGIN")
                .user("admin")
                .device("server-dc")
                .ipAddress("172.16.0.2")
                .destinationIp("172.16.0.100")
                .fileName("cmd.exe")
                .action("EXECUTE")
                .url("https://example.com")
                .port(22)
                .bytes(84200L)
                .sourceEvidenceId("ev-01")
                .description("Sample description")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("event_id")).isTrue();
        assertThat(node.has("case_id")).isTrue();
        assertThat(node.has("timestamp")).isTrue();
        assertThat(node.has("event_type")).isTrue();
        assertThat(node.has("user")).isTrue();
        assertThat(node.has("device")).isTrue();
        assertThat(node.has("ip_address")).isTrue();
        assertThat(node.has("destination_ip")).isTrue();
        assertThat(node.has("file_name")).isTrue();
        assertThat(node.has("action")).isTrue();
        assertThat(node.has("url")).isTrue();
        assertThat(node.has("port")).isTrue();
        assertThat(node.has("bytes")).isTrue();
        assertThat(node.has("source_evidence_id")).isTrue();
        assertThat(node.has("description")).isTrue();

        // Verify values
        assertThat(node.get("event_id").asText()).isEqualTo("urn:uuid:test-id");
        assertThat(node.get("timestamp").asText()).isEqualTo("2026-10-26T08:30:00");
        assertThat(node.get("port").asInt()).isEqualTo(22);
        assertThat(node.get("bytes").asLong()).isEqualTo(84200L);
    }
}
