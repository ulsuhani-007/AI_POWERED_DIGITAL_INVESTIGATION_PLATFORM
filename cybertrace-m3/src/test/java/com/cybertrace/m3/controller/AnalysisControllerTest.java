package com.cybertrace.m3.controller;

import com.cybertrace.m3.model.*;
import com.cybertrace.m3.service.AnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalysisService analysisService;

    private NormalizedEventDTO createEvent(String eventId, String type, LocalDateTime timestamp, Long bytes, Integer port) {
        return NormalizedEventDTO.builder()
                .eventId(eventId)
                .caseId("case-001")
                .timestamp(timestamp != null ? timestamp : LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .eventType(type)
                .user("analyst")
                .device("WORKSTATION-01")
                .ipAddress("192.168.1.100")
                .destinationIp("10.0.0.1")
                .port(port)
                .bytes(bytes)
                .sourceEvidenceId("ev-source-01")
                .description("Normal activity")
                .build();
    }

    private AnalysisReportDTO createSampleReport(int totalEvents, int totalAnomalies) {
        AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                .eventId("ev-anomaly-01")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .eventType("NETWORK_EVENT")
                .anomalyType(AnomalyType.NETWORK_VOLUME)
                .reason("Large outbound transfer detected")
                .severity(AnomalySeverity.HIGH)
                .score(0.88)
                .detectionMethod("STATISTICAL_MAD")
                .build();

        return AnalysisReportDTO.builder()
                .totalEvents(totalEvents)
                .totalAnomalies(totalAnomalies)
                .findings(List.of(finding))
                .statistics(Map.of(
                        "total_events", totalEvents,
                        "events_by_type", Map.of("NETWORK_EVENT", totalEvents),
                        "findings_by_anomaly_type", Map.of("NETWORK_VOLUME", 1),
                        "findings_by_severity", Map.of("HIGH", 1)
                ))
                .summary("Overall Analysis:\n" + totalEvents + " events analyzed across the timeline; " + totalAnomalies + " anomaly identified.\n\n" +
                        "Finding Breakdown:\n- Anomaly Types: NETWORK_VOLUME (1)\n- Severity Distribution: HIGH (1)\n\n" +
                        "Key Findings:\n- [HIGH] NETWORK_VOLUME on event 'ev-anomaly-01' at 2026-10-26T10:30:00: Large outbound transfer detected (Score: 0.8800)\n\n" +
                        "Investigation Conclusion:\nSuspicious activity identified requiring analyst review.")
                .build();
    }

    @Test
    @DisplayName("1. POST /api/v1/analysis/analyze with valid event list returns HTTP 200 and AnalysisReportDTO")
    void testAnalyzeValidEventListReturnsHttp200() throws Exception {
        NormalizedEventDTO event = createEvent("ev-001", "NETWORK_EVENT", null, 1500L, 443);
        AnalysisReportDTO report = createSampleReport(1, 1);

        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(event))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total_events").value(1))
                .andExpect(jsonPath("$.total_anomalies").value(1));

        verify(analysisService).analyze(any());
    }

    @Test
    @DisplayName("2. POST /api/v1/analysis/analyze with empty list returns HTTP 200 and empty report")
    void testAnalyzeEmptyListReturnsHttp200() throws Exception {
        AnalysisReportDTO emptyReport = AnalysisReportDTO.builder()
                .totalEvents(0)
                .totalAnomalies(0)
                .findings(Collections.emptyList())
                .statistics(Map.of("total_events", 0))
                .summary("Overall Analysis:\n0 events analyzed across the timeline; 0 anomalies identified.\n\nInvestigation Conclusion:\nNo anomalies detected.")
                .build();

        when(analysisService.analyze(any())).thenReturn(emptyReport);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_events").value(0))
                .andExpect(jsonPath("$.total_anomalies").value(0))
                .andExpect(jsonPath("$.findings").isEmpty());
    }

    @Test
    @DisplayName("3. POST /api/v1/analysis/analyze handles null/empty request body safely")
    void testAnalyzeNullBodyReturnsHttp200() throws Exception {
        AnalysisReportDTO emptyReport = AnalysisReportDTO.builder()
                .totalEvents(0)
                .totalAnomalies(0)
                .findings(Collections.emptyList())
                .statistics(Map.of("total_events", 0))
                .summary("No anomalies detected.")
                .build();

        when(analysisService.analyze(any())).thenReturn(emptyReport);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_events").value(0))
                .andExpect(jsonPath("$.total_anomalies").value(0));
    }

    @Test
    @DisplayName("4. POST /api/v1/analysis/analyze accepts multiple event types in batch")
    void testAnalyzeMultipleEventTypes() throws Exception {
        List<NormalizedEventDTO> events = List.of(
                createEvent("ev-login", "LOGIN", null, null, null),
                createEvent("ev-file", "FILE_ACCESS", null, 500L, null),
                createEvent("ev-net", "NETWORK_EVENT", null, 2048L, 80),
                createEvent("ev-browser", "BROWSER_ACTIVITY", null, null, null)
        );

        AnalysisReportDTO report = createSampleReport(4, 1);
        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(events)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_events").value(4));
    }

    @Test
    @DisplayName("5. Response contains total_events")
    void testResponseContainsTotalEvents() throws Exception {
        AnalysisReportDTO report = createSampleReport(15, 0);
        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_events").value(15));
    }

    @Test
    @DisplayName("6. Response contains total_anomalies")
    void testResponseContainsTotalAnomalies() throws Exception {
        AnalysisReportDTO report = createSampleReport(15, 3);
        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_anomalies").value(3));
    }

    @Test
    @DisplayName("7. Response contains findings array with accurate field contract")
    void testResponseContainsFindingsArray() throws Exception {
        AnalysisReportDTO report = createSampleReport(10, 1);
        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings").isArray())
                .andExpect(jsonPath("$.findings[0].event_id").value("ev-anomaly-01"))
                .andExpect(jsonPath("$.findings[0].event_type").value("NETWORK_EVENT"))
                .andExpect(jsonPath("$.findings[0].anomaly_type").value("NETWORK_VOLUME"))
                .andExpect(jsonPath("$.findings[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.findings[0].score").value(0.88))
                .andExpect(jsonPath("$.findings[0].detection_method").value("STATISTICAL_MAD"));
    }

    @Test
    @DisplayName("8. Response contains statistics map")
    void testResponseContainsStatistics() throws Exception {
        AnalysisReportDTO report = createSampleReport(10, 1);
        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics").isMap())
                .andExpect(jsonPath("$.statistics.total_events").value(10))
                .andExpect(jsonPath("$.statistics.findings_by_anomaly_type.NETWORK_VOLUME").value(1))
                .andExpect(jsonPath("$.statistics.findings_by_severity.HIGH").value(1));
    }

    @Test
    @DisplayName("9. Response contains generated forensic summary")
    void testResponseContainsGeneratedSummary() throws Exception {
        AnalysisReportDTO report = createSampleReport(10, 1);
        when(analysisService.analyze(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isString())
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("Overall Analysis:")))
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("Investigation Conclusion:")));
    }

    @Test
    @DisplayName("10. Malformed JSON payload returns HTTP 400 Bad Request")
    void testMalformedJsonReturnsHttp400() throws Exception {
        String malformedJson = "{ this is not a valid JSON array }";

        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Unsupported Content-Type returns HTTP 415 Unsupported Media Type")
    void testUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/analyze")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
