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
}
