package com.investigation.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void testEventDtoSnakeCaseMapping() throws Exception {
        String json = """
                {
                    "id": "evt-999",
                    "case_id": "case-891",
                    "evidence_id": "ev-101",
                    "timestamp": "2026-10-27T10:00:00Z",
                    "source": "firewall",
                    "event_type": "DATA_EXFILTRATION",
                    "user": "bad_actor",
                    "ip_address": "198.51.100.42",
                    "device": "DESKTOP-01",
                    "success": true,
                    "bytes": 5000000,
                    "description": "Massive egress stream"
                }
                """;

        EventDto dto = objectMapper.readValue(json, EventDto.class);
        assertEquals("evt-999", dto.getId());
        assertEquals("case-891", dto.getCaseId());
        assertEquals("ev-101", dto.getEvidenceId());
        assertEquals("DATA_EXFILTRATION", dto.getEventType());
        assertEquals("bad_actor", dto.getUser());
        assertEquals("198.51.100.42", dto.getIpAddress());
        assertEquals("DESKTOP-01", dto.getDevice());
        assertTrue(dto.getSuccess());
        assertEquals(5000000L, dto.getBytes());
    }

    @Test
    void testFindingDtoSnakeCaseMapping() throws Exception {
        String json = """
                {
                    "event_id": "evt-999",
                    "timestamp": "2026-10-27T10:00:00Z",
                    "event_type": "DATA_EXFILTRATION",
                    "reason": "Anomaly detected by ML",
                    "severity": "CRITICAL",
                    "score": 0.98,
                    "detection_method": "Isolation Forest"
                }
                """;

        FindingDto dto = objectMapper.readValue(json, FindingDto.class);
        assertEquals("evt-999", dto.getEventId());
        assertEquals("CRITICAL", dto.getSeverity());
        assertEquals(0.98, dto.getScore(), 0.001);
        assertEquals("Isolation Forest", dto.getDetectionMethod());
    }

    @Test
    void testAnalysisDtoMapping() throws Exception {
        String json = """
                {
                    "case_id": "case-891",
                    "total_events": 1420,
                    "total_anomalies": 8,
                    "summary": "AI summary text"
                }
                """;

        AnalysisDto dto = objectMapper.readValue(json, AnalysisDto.class);
        assertEquals("case-891", dto.getCaseId());
        assertEquals(1420, dto.getTotalEvents());
        assertEquals(8, dto.getTotalAnomalies());
        assertEquals("AI summary text", dto.getSummary());
    }

    @Test
    void testEventDtoM1M2ContractCompatibility() throws Exception {
        // M1 / M2 sends event_id and local datetime without trailing Z
        String json = """
                {
                    "event_id": "evt-m1-42",
                    "case_id": "case-100",
                    "source_evidence_id": "ev-50",
                    "timestamp": "2026-10-26T10:30:15",
                    "source": "firewall.log",
                    "event_type": "PORT_SCAN",
                    "user": "root",
                    "ip_address": "192.168.1.100",
                    "device": "SRV-01",
                    "success": false,
                    "bytes_transferred": 1024,
                    "description": "Port scan detected"
                }
                """;

        EventDto dto = objectMapper.readValue(json, EventDto.class);
        assertEquals("evt-m1-42", dto.getId());
        assertEquals("case-100", dto.getCaseId());
        assertEquals("ev-50", dto.getEvidenceId());
        assertNotNull(dto.getTimestamp());
        assertEquals("PORT_SCAN", dto.getEventType());
        assertEquals("192.168.1.100", dto.getIpAddress());
        assertEquals(1024L, dto.getBytes());
    }

    @Test
    void testFindingDtoM3ContractCompatibility() throws Exception {
        // M3 sends anomaly_type, event_id, detection_method, and local datetime
        String json = """
                {
                    "event_id": "evt-m1-42",
                    "timestamp": "2026-10-26T10:30:15",
                    "anomaly_type": "LARGE_DATA_TRANSFER",
                    "reason": "Exceeded statistical baseline threshold",
                    "severity": "HIGH",
                    "score": 0.88,
                    "detection_method": "Isolation Forest"
                }
                """;

        FindingDto dto = objectMapper.readValue(json, FindingDto.class);
        assertEquals("evt-m1-42", dto.getEventId());
        assertEquals("fnd-evt-m1-42", dto.getId());
        assertEquals("LARGE_DATA_TRANSFER", dto.getEventType());
        assertEquals("HIGH", dto.getSeverity());
        assertEquals(0.88, dto.getScore(), 0.001);
        assertNotNull(dto.getTimestamp());
    }

    @Test
    void testCaseDtoAndEvidenceDtoFlexibleDates() throws Exception {
        String caseJson = """
                {
                    "id": "case-uuid-1",
                    "caseNumber": "CASE-2026-001",
                    "title": "Operation Nightfall",
                    "createdAt": "2026-10-26T09:00:00",
                    "updatedAt": "2026-10-26T10:00:00"
                }
                """;
        CaseDto cDto = objectMapper.readValue(caseJson, CaseDto.class);
        assertEquals("CASE-2026-001", cDto.getCaseNumber());
        assertNotNull(cDto.getCreatedAt());
        assertNotNull(cDto.getUpdatedAt());

        String evJson = """
                {
                    "id": "ev-uuid-1",
                    "caseId": "case-uuid-1",
                    "fileName": "auth.log",
                    "uploadedAt": "2026-10-26T09:30:00",
                    "hashSha256": "abcdef123456"
                }
                """;
        EvidenceDto evDto = objectMapper.readValue(evJson, EvidenceDto.class);
        assertEquals("auth.log", evDto.getFileName());
        assertNotNull(evDto.getUploadedAt());
        assertEquals("abcdef123456", evDto.getHashSha256());
    }

    @Test
    void testEvidenceDtoUploadedByField() throws Exception {
        String evJson = """
                {
                    "id": "ev-uuid-2",
                    "caseId": "case-uuid-1",
                    "fileName": "network.pcap",
                    "uploadedBy": "forensic_expert",
                    "hashSha256": "1234567890abcdef"
                }
                """;
        EvidenceDto evDto = objectMapper.readValue(evJson, EvidenceDto.class);
        assertEquals("forensic_expert", evDto.getUploadedBy());
    }

    @Test
    void testUserDtoSerialization() throws Exception {
        String userJson = """
                {
                    "id": "u-admin-123",
                    "username": "superadmin",
                    "role": "ADMIN",
                    "enabled": true,
                    "createdAt": "2026-10-26T09:00:00Z"
                }
                """;
        UserDto userDto = objectMapper.readValue(userJson, UserDto.class);
        assertEquals("u-admin-123", userDto.getId());
        assertEquals("superadmin", userDto.getUsername());
        assertEquals("ADMIN", userDto.getRole());
        assertTrue(userDto.isEnabled());
        assertNotNull(userDto.getCreatedAt());
    }
}
