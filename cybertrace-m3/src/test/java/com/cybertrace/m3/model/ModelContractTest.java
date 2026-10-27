package com.cybertrace.m3.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelContractTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("1. AnomalySeverity enum defines required levels")
    void testAnomalySeverityEnum() {
        assertThat(AnomalySeverity.values()).containsExactly(
                AnomalySeverity.LOW,
                AnomalySeverity.MEDIUM,
                AnomalySeverity.HIGH,
                AnomalySeverity.CRITICAL
        );
    }

    @Test
    @DisplayName("2. AnomalyType enum defines required categories")
    void testAnomalyTypeEnum() {
        assertThat(AnomalyType.values()).containsExactly(
                AnomalyType.BRUTE_FORCE,
                AnomalyType.NETWORK_VOLUME,
                AnomalyType.OFF_HOURS,
                AnomalyType.MACHINE_LEARNING
        );
    }

    @Test
    @DisplayName("3. AnomalyFindingDTO JSON serialization and deserialization")
    void testAnomalyFindingDTOJsonContract() throws Exception {
        AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                .eventId("urn:uuid:1111-2222-3333-4444")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 35, 0))
                .eventType("LOGIN")
                .anomalyType(AnomalyType.BRUTE_FORCE)
                .reason("Multiple consecutive failed login attempts detected for user 'attacker'")
                .severity(AnomalySeverity.HIGH)
                .score(0.85)
                .detectionMethod("RULE_BASED")
                .build();

        String json = objectMapper.writeValueAsString(finding);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("event_id")).isTrue();
        assertThat(node.has("timestamp")).isTrue();
        assertThat(node.has("event_type")).isTrue();
        assertThat(node.has("anomaly_type")).isTrue();
        assertThat(node.has("reason")).isTrue();
        assertThat(node.has("severity")).isTrue();
        assertThat(node.has("score")).isTrue();
        assertThat(node.has("detection_method")).isTrue();

        assertThat(node.get("anomaly_type").asText()).isEqualTo("BRUTE_FORCE");
        assertThat(node.get("severity").asText()).isEqualTo("HIGH");
        assertThat(node.get("score").asDouble()).isEqualTo(0.85);

        AnomalyFindingDTO deserialized = objectMapper.readValue(json, AnomalyFindingDTO.class);
        assertThat(deserialized).isEqualTo(finding);
    }

    @Test
    @DisplayName("4. AnalysisReportDTO JSON serialization and deserialization")
    void testAnalysisReportDTOJsonContract() throws Exception {
        AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                .eventId("urn:uuid:exfil-01")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 39, 0))
                .eventType("NETWORK_EVENT")
                .anomalyType(AnomalyType.NETWORK_VOLUME)
                .reason("Outbound traffic spike (4500000 bytes) to external IP 185.220.101.5")
                .severity(AnomalySeverity.CRITICAL)
                .score(0.95)
                .detectionMethod("VOLUME_THRESHOLD")
                .build();

        AnalysisReportDTO report = AnalysisReportDTO.builder()
                .totalEvents(16)
                .totalAnomalies(1)
                .findings(List.of(finding))
                .statistics(Map.of(
                        "total_logins", 4,
                        "failed_logins", 2,
                        "network_bytes_transferred", 4600000
                ))
                .summary("Detected potential brute force and data exfiltration activity.")
                .build();

        String json = objectMapper.writeValueAsString(report);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("total_events")).isTrue();
        assertThat(node.has("total_anomalies")).isTrue();
        assertThat(node.has("findings")).isTrue();
        assertThat(node.has("statistics")).isTrue();
        assertThat(node.has("summary")).isTrue();

        assertThat(node.get("total_events").asInt()).isEqualTo(16);
        assertThat(node.get("total_anomalies").asInt()).isEqualTo(1);
        assertThat(node.get("findings").size()).isEqualTo(1);

        AnalysisReportDTO deserialized = objectMapper.readValue(json, AnalysisReportDTO.class);
        assertThat(deserialized).isEqualTo(report);
    }
}
