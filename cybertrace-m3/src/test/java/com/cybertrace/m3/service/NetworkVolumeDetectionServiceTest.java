package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkVolumeDetectionServiceTest {

    private NetworkVolumeDetectionService service;

    @BeforeEach
    void setUp() {
        // Default: modified Z threshold = 3.5, min sample size = 3
        service = new NetworkVolumeDetectionService(3.5, 3);
    }

    private NormalizedEventDTO createNetworkEvent(String id, Long bytes, LocalDateTime timestamp) {
        return NormalizedEventDTO.builder()
                .eventId(id != null ? id : "urn:uuid:" + UUID.randomUUID())
                .caseId("case-test")
                .timestamp(timestamp != null ? timestamp : LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .eventType("NETWORK_EVENT")
                .ipAddress("192.168.1.10")
                .destinationIp("185.220.101.5")
                .port(8080)
                .bytes(bytes)
                .sourceEvidenceId("ev-net-01")
                .description("Network traffic")
                .build();
    }

    @Test
    @DisplayName("1. A clearly extreme network transfer is detected")
    void testExtremeNetworkTransferDetected() {
        // Baseline of normal traffic (1000 - 3000 bytes) and one extreme spike (10,000,000 bytes)
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 1000L, null),
                createNetworkEvent("ev-2", 1200L, null),
                createNetworkEvent("ev-3", 1100L, null),
                createNetworkEvent("ev-4", 1500L, null),
                createNetworkEvent("ev-5", 1300L, null),
                createNetworkEvent("ev-6", 10_000_000L, null) // Extreme spike
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        AnomalyFindingDTO finding = findings.get(0);

        assertThat(finding.getEventId()).isEqualTo("ev-6");
        assertThat(finding.getEventType()).isEqualTo("NETWORK_EVENT");
        assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.NETWORK_VOLUME);
        assertThat(finding.getDetectionMethod()).isEqualTo("STATISTICAL_MEDIAN_ABSOLUTE_DEVIATION");
        assertThat(finding.getSeverity()).isEqualTo(AnomalySeverity.CRITICAL);
        assertThat(finding.getScore()).isGreaterThanOrEqualTo(0.90);
        assertThat(finding.getReason())
                .contains("10000000")
                .contains("unusually high")
                .contains("median");
    }

    @Test
    @DisplayName("2. Normal network transfers are not flagged")
    void testNormalNetworkTransfersNotFlagged() {
        // Homogeneous network traffic within normal fluctuations
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 1000L, null),
                createNetworkEvent("ev-2", 1050L, null),
                createNetworkEvent("ev-3", 980L, null),
                createNetworkEvent("ev-4", 1120L, null),
                createNetworkEvent("ev-5", 1020L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("3. Missing bytes are ignored")
    void testMissingBytesIgnored() {
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", null, null), // Null bytes
                createNetworkEvent("ev-2", -50L, null),  // Negative bytes
                createNetworkEvent("ev-3", 1000L, null),
                createNetworkEvent("ev-4", 1050L, null),
                createNetworkEvent("ev-5", 1000L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        // Neither null nor negative bytes cause exceptions; normal events not flagged
        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("4. Non-network events are ignored")
    void testNonNetworkEventsIgnored() {
        NormalizedEventDTO loginEvent = NormalizedEventDTO.builder()
                .eventId("login-1")
                .caseId("c-1")
                .eventType("LOGIN")
                .bytes(999_999_999L) // High bytes but LOGIN event
                .build();

        NormalizedEventDTO fileEvent = NormalizedEventDTO.builder()
                .eventId("file-1")
                .caseId("c-1")
                .eventType("FILE_ACCESS")
                .bytes(888_888_888L) // High bytes but FILE event
                .build();

        List<NormalizedEventDTO> events = List.of(loginEvent, fileEvent);

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("5. Empty input returns no findings")
    void testEmptyInputReturnsNoFindings() {
        assertThat(service.detect(Collections.emptyList())).isEmpty();
        assertThat(service.detect(null)).isEmpty();
    }

    @Test
    @DisplayName("6. A single network event is handled safely")
    void testSingleNetworkEventHandledSafely() {
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 5000000L, null)
        );

        // Less than minSampleSize (3), cannot establish statistical baseline
        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("7. Identical byte values do not cause an error or false positives")
    void testIdenticalByteValuesHandledSafely() {
        // All events have identical byte size (MAD = 0, scale = 0)
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 500L, null),
                createNetworkEvent("ev-2", 500L, null),
                createNetworkEvent("ev-3", 500L, null),
                createNetworkEvent("ev-4", 500L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("8. Unsorted events are handled correctly")
    void testUnsortedEventsHandledCorrectly() {
        // Shuffled input with the anomaly in the middle
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 1500L, null),
                createNetworkEvent("ev-spike", 25_000_000L, null), // Spike
                createNetworkEvent("ev-2", 1100L, null),
                createNetworkEvent("ev-3", 1200L, null),
                createNetworkEvent("ev-4", 1000L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getEventId()).isEqualTo("ev-spike");
    }

    @Test
    @DisplayName("9. Finding preserves the original M2 event_id")
    void testFindingPreservesOriginalEventId() {
        String originalId = "urn:uuid:89ab-cdef-0123-4567";
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 200L, null),
                createNetworkEvent("ev-2", 210L, null),
                createNetworkEvent("ev-3", 190L, null),
                createNetworkEvent(originalId, 8_000_000L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getEventId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("10. Score is always strictly between 0.0 and 1.0")
    void testScoreIsAlwaysBetweenZeroAndOne() {
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 100L, null),
                createNetworkEvent("ev-2", 100L, null),
                createNetworkEvent("ev-3", 120L, null),
                createNetworkEvent("ev-4", 110L, null),
                createNetworkEvent("ev-huge", 100_000_000_000L, null) // Astronomical transfer
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        double score = findings.get(0).getScore();
        assertThat(score).isGreaterThanOrEqualTo(0.0);
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("11. Severity is sensible for different anomaly magnitudes")
    void testGraduatedSeverity() {
        // Baseline: ~1000 bytes, MAD ~ 100, scale ~ 148.26
        // Anomaly 1: ~1600 bytes -> (1600-1000)/148.26 ~= 4.04 (MEDIUM)
        // Anomaly 2: ~2200 bytes -> (2200-1000)/148.26 ~= 8.09 (HIGH)
        // Anomaly 3: ~3000 bytes -> (3000-1000)/148.26 ~= 13.48 (CRITICAL)
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("base-1", 900L, null),
                createNetworkEvent("base-2", 1000L, null),
                createNetworkEvent("base-3", 1000L, null),
                createNetworkEvent("base-4", 1100L, null),
                createNetworkEvent("anom-med", 1600L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);

        // High severity test
        List<NormalizedEventDTO> eventsHigh = List.of(
                createNetworkEvent("base-1", 900L, null),
                createNetworkEvent("base-2", 1000L, null),
                createNetworkEvent("base-3", 1000L, null),
                createNetworkEvent("base-4", 1100L, null),
                createNetworkEvent("anom-high", 2200L, null)
        );
        List<AnomalyFindingDTO> findingsHigh = service.detect(eventsHigh);
        assertThat(findingsHigh).hasSize(1);
        assertThat(findingsHigh.get(0).getSeverity()).isEqualTo(AnomalySeverity.HIGH);

        // Critical severity test
        List<NormalizedEventDTO> eventsCrit = List.of(
                createNetworkEvent("base-1", 900L, null),
                createNetworkEvent("base-2", 1000L, null),
                createNetworkEvent("base-3", 1000L, null),
                createNetworkEvent("base-4", 1100L, null),
                createNetworkEvent("anom-crit", 3000L, null)
        );
        List<AnomalyFindingDTO> findingsCrit = service.detect(eventsCrit);
        assertThat(findingsCrit).hasSize(1);
        assertThat(findingsCrit.get(0).getSeverity()).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    @DisplayName("12. Multiple unusual network events are handled without crashing")
    void testMultipleUnusualNetworkEventsHandled() {
        List<NormalizedEventDTO> events = List.of(
                createNetworkEvent("ev-1", 500L, null),
                createNetworkEvent("ev-2", 520L, null),
                createNetworkEvent("ev-3", 480L, null),
                createNetworkEvent("ev-4", 510L, null),
                createNetworkEvent("spike-1", 1_000_000L, null),
                createNetworkEvent("spike-2", 2_500_000L, null)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(AnomalyFindingDTO::getEventId)
                .containsExactlyInAnyOrder("spike-1", "spike-2");
    }

    @Test
    @DisplayName("13. Detects actual M2 sample network_events.csv anomaly (4,500,000 bytes)")
    void testDetectsActualM2SampleNetworkAnomaly() {
        // Values from cybertrace-m2/src/test/resources/samples/network_events.csv
        List<NormalizedEventDTO> actualM2Events = List.of(
                createNetworkEvent("m2-row2", 15420L, LocalDateTime.of(2026, 10, 26, 10, 30, 15)),
                createNetworkEvent("m2-row3", 128L, LocalDateTime.of(2026, 10, 26, 10, 31, 0)),
                createNetworkEvent("m2-row4", 84200L, LocalDateTime.of(2026, 10, 26, 10, 36, 0)),
                createNetworkEvent("m2-row5-exfil", 4500000L, LocalDateTime.of(2026, 10, 26, 10, 39, 0))
        );

        List<AnomalyFindingDTO> findings = service.detect(actualM2Events);

        assertThat(findings).hasSize(1);
        AnomalyFindingDTO finding = findings.get(0);
        assertThat(finding.getEventId()).isEqualTo("m2-row5-exfil");
        assertThat(finding.getSeverity()).isEqualTo(AnomalySeverity.CRITICAL);
        assertThat(finding.getScore()).isEqualTo(0.99);
        assertThat(finding.getReason()).contains("4500000 bytes");
    }
}
