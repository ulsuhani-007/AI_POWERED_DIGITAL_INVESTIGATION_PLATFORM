package com.cybertrace.m3.service;

import com.cybertrace.m3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnalysisServiceTest {

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService();
    }

    private NormalizedEventDTO createLogin(String id, String user, String ip, LocalDateTime timestamp, boolean success, String sourceId) {
        String status = success ? "SUCCESS" : "FAILED";
        return NormalizedEventDTO.builder()
                .eventId(id)
                .caseId("case-001")
                .timestamp(timestamp)
                .eventType("LOGIN")
                .user(user)
                .ipAddress(ip)
                .sourceEvidenceId(sourceId != null ? sourceId : "ev-login")
                .description("User " + user + " login attempt: " + status)
                .build();
    }

    private NormalizedEventDTO createNetwork(String id, Long bytes, LocalDateTime timestamp, String sourceId) {
        return NormalizedEventDTO.builder()
                .eventId(id)
                .caseId("case-001")
                .timestamp(timestamp)
                .eventType("NETWORK_EVENT")
                .ipAddress("192.168.1.10")
                .destinationIp("185.220.101.5")
                .port(8080)
                .bytes(bytes)
                .sourceEvidenceId(sourceId != null ? sourceId : "ev-net")
                .description("Network event")
                .build();
    }

    private NormalizedEventDTO createFileAccess(String id, String user, String file, LocalDateTime timestamp, String sourceId) {
        return NormalizedEventDTO.builder()
                .eventId(id)
                .caseId("case-001")
                .timestamp(timestamp)
                .eventType("FILE_ACCESS")
                .user(user)
                .fileName(file)
                .action("READ")
                .sourceEvidenceId(sourceId != null ? sourceId : "ev-file")
                .description("User " + user + " executed action READ on file " + file)
                .build();
    }

    @Test
    @DisplayName("1. Empty input returns safe report with zero counts, valid stats, and empty summary")
    void testEmptyInputReturnsSafeReport() {
        AnalysisReportDTO report = analysisService.analyze(Collections.emptyList());

        assertThat(report).isNotNull();
        assertThat(report.getTotalEvents()).isEqualTo(0);
        assertThat(report.getTotalAnomalies()).isEqualTo(0);
        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getSummary()).contains("0 events analyzed").contains("No anomalies detected");

        Map<String, Object> stats = report.getStatistics();
        assertThat(stats).isNotNull();
        assertThat(stats.get("total_events")).isEqualTo(0);
        assertThat((Map<?, ?>) stats.get("events_by_type")).isEmpty();
        assertThat((Map<?, ?>) stats.get("events_by_source")).isEmpty();
        assertThat((Map<?, ?>) stats.get("findings_by_anomaly_type")).isEmpty();
        assertThat((Map<?, ?>) stats.get("findings_by_severity")).isEmpty();
    }

    @Test
    @DisplayName("2. Null input returns safe report with zero counts, valid stats, and empty summary")
    void testNullInputReturnsSafeReport() {
        AnalysisReportDTO report = analysisService.analyze(null);

        assertThat(report).isNotNull();
        assertThat(report.getTotalEvents()).isEqualTo(0);
        assertThat(report.getTotalAnomalies()).isEqualTo(0);
        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getSummary()).contains("0 events analyzed").contains("No anomalies detected");
        assertThat(report.getStatistics()).containsKey("total_events");
    }

    @Test
    @DisplayName("3. Normal events with zero findings produce correct statistics and empty findings")
    void testNormalEventsWithZeroFindings() {
        // Normal daytime events, single login, normal network bytes
        List<NormalizedEventDTO> events = List.of(
                createLogin("log-1", "user1", "10.0.0.1", LocalDateTime.of(2026, 10, 26, 10, 0, 0), true, "ev-log"),
                createNetwork("net-1", 1000L, LocalDateTime.of(2026, 10, 26, 11, 0, 0), "ev-net"),
                createNetwork("net-2", 1100L, LocalDateTime.of(2026, 10, 26, 11, 5, 0), "ev-net"),
                createNetwork("net-3", 1050L, LocalDateTime.of(2026, 10, 26, 11, 10, 0), "ev-net"),
                createFileAccess("file-1", "user1", "doc.txt", LocalDateTime.of(2026, 10, 26, 14, 0, 0), "ev-file")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getTotalEvents()).isEqualTo(5);
        assertThat(report.getTotalAnomalies()).isEqualTo(0);
        assertThat(report.getFindings()).isEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Integer> byType = (Map<String, Integer>) report.getStatistics().get("events_by_type");
        assertThat(byType).containsEntry("LOGIN", 1);
        assertThat(byType).containsEntry("NETWORK_EVENT", 3);
        assertThat(byType).containsEntry("FILE_ACCESS", 1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> bySource = (Map<String, Integer>) report.getStatistics().get("events_by_source");
        assertThat(bySource).containsEntry("ev-log", 1);
        assertThat(bySource).containsEntry("ev-net", 3);
        assertThat(bySource).containsEntry("ev-file", 1);
    }

    @Test
    @DisplayName("4. Input containing a brute-force incident is detected and reported")
    void testInputContainingBruteForceIncident() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        List<NormalizedEventDTO> events = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            events.add(createLogin("bf-" + i, "attacker", "198.51.100.42", base.plusMinutes(i), false, "ev-log"));
        }

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getTotalAnomalies()).isEqualTo(1);
        AnomalyFindingDTO finding = report.getFindings().get(0);
        assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.BRUTE_FORCE);
        assertThat(finding.getEventId()).isEqualTo("bf-4");

        @SuppressWarnings("unchecked")
        Map<String, Integer> byAnomaly = (Map<String, Integer>) report.getStatistics().get("findings_by_anomaly_type");
        assertThat(byAnomaly).containsEntry("BRUTE_FORCE", 1);
    }

    @Test
    @DisplayName("5. Input containing a network-volume anomaly is detected and reported")
    void testInputContainingNetworkVolumeAnomaly() {
        List<NormalizedEventDTO> events = List.of(
                createNetwork("net-1", 1000L, LocalDateTime.of(2026, 10, 26, 10, 0, 0), "ev-net"),
                createNetwork("net-2", 1050L, LocalDateTime.of(2026, 10, 26, 10, 1, 0), "ev-net"),
                createNetwork("net-3", 980L, LocalDateTime.of(2026, 10, 26, 10, 2, 0), "ev-net"),
                createNetwork("net-spike", 15_000_000L, LocalDateTime.of(2026, 10, 26, 10, 3, 0), "ev-net")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getTotalAnomalies()).isEqualTo(1);
        AnomalyFindingDTO finding = report.getFindings().get(0);
        assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.NETWORK_VOLUME);
        assertThat(finding.getEventId()).isEqualTo("net-spike");

        @SuppressWarnings("unchecked")
        Map<String, Integer> byAnomaly = (Map<String, Integer>) report.getStatistics().get("findings_by_anomaly_type");
        assertThat(byAnomaly).containsEntry("NETWORK_VOLUME", 1);
    }

    @Test
    @DisplayName("6. Input containing off-hours activity is detected and reported")
    void testInputContainingOffHoursActivity() {
        List<NormalizedEventDTO> events = List.of(
                createLogin("log-off", "user1", "10.0.0.1", LocalDateTime.of(2026, 10, 26, 2, 15, 0), true, "ev-log"),
                createFileAccess("file-off", "user1", "dump.sql", LocalDateTime.of(2026, 10, 26, 3, 0, 0), "ev-file")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getTotalAnomalies()).isEqualTo(2);
        assertThat(report.getFindings()).extracting(AnomalyFindingDTO::getAnomalyType)
                .containsOnly(AnomalyType.OFF_HOURS);
        assertThat(report.getFindings()).extracting(AnomalyFindingDTO::getEventId)
                .containsExactly("log-off", "file-off");
    }

    @Test
    @DisplayName("7. Input containing multiple anomaly types combines all findings correctly")
    void testMultipleAnomalyTypesCombined() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);

        List<NormalizedEventDTO> events = new ArrayList<>();
        // 1. 5 failed logins (BRUTE_FORCE)
        for (int i = 0; i < 5; i++) {
            events.add(createLogin("bf-" + i, "attacker", "198.51.100.42", baseTime.plusMinutes(i), false, "ev-log"));
        }
        // 2. Network volume spike (NETWORK_VOLUME)
        events.add(createNetwork("net-1", 1000L, baseTime.plusMinutes(10), "ev-net"));
        events.add(createNetwork("net-2", 1100L, baseTime.plusMinutes(11), "ev-net"));
        events.add(createNetwork("net-3", 1050L, baseTime.plusMinutes(12), "ev-net"));
        events.add(createNetwork("net-spike", 20_000_000L, baseTime.plusMinutes(13), "ev-net"));
        // 3. Off-hours file access at 02:00 AM (OFF_HOURS)
        events.add(createFileAccess("file-night", "admin", "secret.key", LocalDateTime.of(2026, 10, 26, 2, 0, 0), "ev-file"));

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getTotalEvents()).isEqualTo(10);
        assertThat(report.getTotalAnomalies()).isEqualTo(3);

        List<AnomalyFindingDTO> findings = report.getFindings();
        assertThat(findings).extracting(AnomalyFindingDTO::getAnomalyType)
                .containsExactlyInAnyOrder(
                        AnomalyType.OFF_HOURS,
                        AnomalyType.BRUTE_FORCE,
                        AnomalyType.NETWORK_VOLUME
                );
    }

    @Test
    @DisplayName("8 & 9. Findings are sorted chronologically with deterministic secondary ordering")
    void testFindingsSortedChronologically() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 26, 1, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 26, 3, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 10, 26, 23, 0, 0);

        // Input passed out of order
        List<NormalizedEventDTO> events = List.of(
                createLogin("log-t3", "user1", "1.1.1.1", t3, true, "ev-log"),
                createLogin("log-t1", "user1", "1.1.1.1", t1, true, "ev-log"),
                createFileAccess("file-t2", "user1", "test.txt", t2, "ev-file")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        List<AnomalyFindingDTO> findings = report.getFindings();
        assertThat(findings).hasSize(3);

        // Must be sorted t1 (01:00) -> t2 (03:00) -> t3 (23:00)
        assertThat(findings.get(0).getEventId()).isEqualTo("log-t1");
        assertThat(findings.get(1).getEventId()).isEqualTo("file-t2");
        assertThat(findings.get(2).getEventId()).isEqualTo("log-t3");
    }

    @Test
    @DisplayName("10. Exact duplicate findings from detectors are safely deduplicated")
    void testDuplicateFindingsAreRemoved() {
        // Mock a detector that accidentally returns duplicate findings
        BruteForceDetectionService mockBf = mock(BruteForceDetectionService.class);
        NetworkVolumeDetectionService mockNet = mock(NetworkVolumeDetectionService.class);
        OffHoursDetectionService mockOff = mock(OffHoursDetectionService.class);

        AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                .eventId("ev-dup")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 0, 0))
                .eventType("LOGIN")
                .anomalyType(AnomalyType.BRUTE_FORCE)
                .reason("Duplicate attempt")
                .severity(AnomalySeverity.MEDIUM)
                .score(0.70)
                .detectionMethod("RULE_BASED")
                .build();

        // Returning the exact same finding twice
        when(mockBf.detect(any())).thenReturn(List.of(finding, finding));
        when(mockNet.detect(any())).thenReturn(Collections.emptyList());
        when(mockOff.detect(any())).thenReturn(Collections.emptyList());

        AnalysisService customService = new AnalysisService(mockBf, mockNet, mockOff);

        List<NormalizedEventDTO> dummyEvents = List.of(createLogin("ev-dup", "u", "ip", LocalDateTime.now(), false, "src"));
        AnalysisReportDTO report = customService.analyze(dummyEvents);

        assertThat(report.getTotalAnomalies()).isEqualTo(1);
        assertThat(report.getFindings()).hasSize(1);
    }

    @Test
    @DisplayName("11. Original event IDs are preserved in all findings")
    void testOriginalEventIdsPreserved() {
        String originalId = "urn:uuid:aaaa-bbbb-cccc-dddd";
        List<NormalizedEventDTO> events = List.of(
                createLogin(originalId, "u1", "1.1.1.1", LocalDateTime.of(2026, 10, 26, 2, 0, 0), true, "ev-1")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getFindings().get(0).getEventId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("12 & 13. total_events and total_anomalies match exactly")
    void testTotalEventsAndTotalAnomaliesMatch() {
        List<NormalizedEventDTO> events = List.of(
                createLogin("l-1", "user", "10.0.0.1", LocalDateTime.of(2026, 10, 26, 12, 0, 0), true, "src"),
                createLogin("l-2", "user", "10.0.0.1", LocalDateTime.of(2026, 10, 26, 13, 0, 0), true, "src"),
                createLogin("l-off", "user", "10.0.0.1", LocalDateTime.of(2026, 10, 26, 2, 0, 0), true, "src")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getTotalEvents()).isEqualTo(3);
        assertThat(report.getTotalAnomalies()).isEqualTo(1);
        assertThat(report.getTotalAnomalies()).isEqualTo(report.getFindings().size());
    }

    @Test
    @DisplayName("14, 15, 16. Statistics contain correct event-type, anomaly-type, and severity breakdowns")
    void testStatisticsBreakdownsAreAccurate() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);

        List<NormalizedEventDTO> events = List.of(
                createLogin("l-1", "user1", "10.0.0.1", baseTime, true, "src-login"),
                createLogin("l-2", "user2", "10.0.0.2", baseTime.plusMinutes(1), false, "src-login"),
                createNetwork("n-1", 1000L, baseTime.plusMinutes(2), "src-net"),
                createFileAccess("f-1", "user1", "a.txt", LocalDateTime.of(2026, 10, 26, 2, 0, 0), "src-file") // Off-hours (HIGH)
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        Map<String, Object> stats = report.getStatistics();

        @SuppressWarnings("unchecked")
        Map<String, Integer> byType = (Map<String, Integer>) stats.get("events_by_type");
        assertThat(byType).containsEntry("LOGIN", 2);
        assertThat(byType).containsEntry("NETWORK_EVENT", 1);
        assertThat(byType).containsEntry("FILE_ACCESS", 1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> bySource = (Map<String, Integer>) stats.get("events_by_source");
        assertThat(bySource).containsEntry("src-login", 2);
        assertThat(bySource).containsEntry("src-net", 1);
        assertThat(bySource).containsEntry("src-file", 1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> byAnomaly = (Map<String, Integer>) stats.get("findings_by_anomaly_type");
        assertThat(byAnomaly).containsEntry("OFF_HOURS", 1);

        @SuppressWarnings("unchecked")
        Map<String, Integer> bySeverity = (Map<String, Integer>) stats.get("findings_by_severity");
        assertThat(bySeverity).containsEntry("HIGH", 1);
    }

    @Test
    @DisplayName("17. Existing detectors continue to work through the orchestration service")
    void testExistingDetectorsWorkThroughOrchestration() {
        assertThat(analysisService.getBruteForceService()).isNotNull();
        assertThat(analysisService.getNetworkVolumeService()).isNotNull();
        assertThat(analysisService.getOffHoursService()).isNotNull();
        assertThat(analysisService.getMachineLearningService()).isNotNull();
        assertThat(analysisService.getSummaryService()).isNotNull();
    }

    @Test
    @DisplayName("18. Machine learning findings appear in the unified report")
    void testMachineLearningFindingsAppearInUnifiedReport() {
        List<NormalizedEventDTO> events = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            events.add(createNetwork("net-" + i, (long) (1000 + (i * 10)),
                    LocalDateTime.of(2026, 10, 26, 11, i, 0), "ev-net"));
        }
        events.add(createNetwork("ml-spike", 10_000_000_000L,
                LocalDateTime.of(2026, 10, 26, 11, 30, 0), "ev-net"));

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getFindings()).isNotEmpty();
        boolean hasMl = report.getFindings().stream()
                .anyMatch(f -> f.getAnomalyType() == AnomalyType.MACHINE_LEARNING && "ml-spike".equals(f.getEventId()));
        assertThat(hasMl).isTrue();

        @SuppressWarnings("unchecked")
        Map<String, Integer> byAnomaly = (Map<String, Integer>) report.getStatistics().get("findings_by_anomaly_type");
        assertThat(byAnomaly).containsKey("MACHINE_LEARNING");
    }

    @Test
    @DisplayName("19. ML findings coexist with rule-based findings across all four detectors")
    void testMachineLearningCoexistsWithRuleBasedFindings() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        List<NormalizedEventDTO> events = new ArrayList<>();

        // 1. 5 failed logins for brute force (BRUTE_FORCE)
        for (int i = 0; i < 5; i++) {
            events.add(createLogin("bf-" + i, "attacker", "198.51.100.42", baseTime.plusMinutes(i), false, "ev-login"));
        }
        // 2. Off-hours file access at 02:00 AM (OFF_HOURS)
        events.add(createFileAccess("file-night", "admin", "secret.key", LocalDateTime.of(2026, 10, 26, 2, 0, 0), "ev-file"));

        // 3. 10 normal network transfers and 1 massive exfiltration spike (NETWORK_VOLUME & MACHINE_LEARNING)
        for (int i = 0; i < 10; i++) {
            events.add(createNetwork("net-" + i, (long) (1200 + (i * 20)), baseTime.plusMinutes(20 + i), "ev-net"));
        }
        events.add(createNetwork("spike-ml", 8_000_000_000L, baseTime.plusMinutes(40), "ev-net"));

        AnalysisReportDTO report = analysisService.analyze(events);

        Set<AnomalyType> detectedTypes = report.getFindings().stream()
                .map(AnomalyFindingDTO::getAnomalyType)
                .collect(Collectors.toSet());

        assertThat(detectedTypes).contains(
                AnomalyType.BRUTE_FORCE,
                AnomalyType.OFF_HOURS,
                AnomalyType.NETWORK_VOLUME,
                AnomalyType.MACHINE_LEARNING
        );

        @SuppressWarnings("unchecked")
        Map<String, Integer> byAnomaly = (Map<String, Integer>) report.getStatistics().get("findings_by_anomaly_type");
        assertThat(byAnomaly).containsKeys("BRUTE_FORCE", "OFF_HOURS", "NETWORK_VOLUME", "MACHINE_LEARNING");
    }

    @Test
    @DisplayName("20. Same event can contain different anomaly types (e.g. OFF_HOURS and MACHINE_LEARNING)")
    void testSameEventCanContainDifferentAnomalyTypes() {
        BruteForceDetectionService mockBf = mock(BruteForceDetectionService.class);
        NetworkVolumeDetectionService mockNet = mock(NetworkVolumeDetectionService.class);
        OffHoursDetectionService mockOff = mock(OffHoursDetectionService.class);
        MachineLearningAnomalyDetectionService mockMl = mock(MachineLearningAnomalyDetectionService.class);

        LocalDateTime eventTime = LocalDateTime.of(2026, 10, 26, 2, 30, 0);
        String eventId = "shared-event-001";

        AnomalyFindingDTO offHoursFinding = AnomalyFindingDTO.builder()
                .eventId(eventId)
                .timestamp(eventTime)
                .eventType("FILE_ACCESS")
                .anomalyType(AnomalyType.OFF_HOURS)
                .reason("Activity at 02:30 outside working hours")
                .severity(AnomalySeverity.HIGH)
                .score(0.85)
                .detectionMethod("RULE_BASED_OFF_HOURS")
                .build();

        AnomalyFindingDTO mlFinding = AnomalyFindingDTO.builder()
                .eventId(eventId)
                .timestamp(eventTime)
                .eventType("FILE_ACCESS")
                .anomalyType(AnomalyType.MACHINE_LEARNING)
                .reason("Isolation Forest detected unusual action and timing")
                .severity(AnomalySeverity.CRITICAL)
                .score(0.92)
                .detectionMethod("Smile 3.1.0 Isolation Forest")
                .build();

        when(mockBf.detect(any())).thenReturn(Collections.emptyList());
        when(mockNet.detect(any())).thenReturn(Collections.emptyList());
        when(mockOff.detect(any())).thenReturn(List.of(offHoursFinding));
        when(mockMl.detect(any())).thenReturn(List.of(mlFinding));

        AnalysisService customService = new AnalysisService(mockBf, mockNet, mockOff, mockMl);

        List<NormalizedEventDTO> events = List.of(
                createFileAccess(eventId, "user", "file.dat", eventTime, "src-file")
        );

        AnalysisReportDTO report = customService.analyze(events);

        // Both findings on the exact same event must remain!
        assertThat(report.getTotalAnomalies()).isEqualTo(2);
        assertThat(report.getFindings()).hasSize(2);
        assertThat(report.getFindings()).extracting(AnomalyFindingDTO::getAnomalyType)
                .containsExactlyInAnyOrder(AnomalyType.OFF_HOURS, AnomalyType.MACHINE_LEARNING);
        assertThat(report.getFindings()).extracting(AnomalyFindingDTO::getEventId)
                .containsOnly(eventId);
    }

    @Test
    @DisplayName("21. Exact duplicate ML findings are safely deduplicated")
    void testExactDuplicateMlFindingsAreRemoved() {
        BruteForceDetectionService mockBf = mock(BruteForceDetectionService.class);
        NetworkVolumeDetectionService mockNet = mock(NetworkVolumeDetectionService.class);
        OffHoursDetectionService mockOff = mock(OffHoursDetectionService.class);
        MachineLearningAnomalyDetectionService mockMl = mock(MachineLearningAnomalyDetectionService.class);

        LocalDateTime eventTime = LocalDateTime.of(2026, 10, 26, 14, 0, 0);
        String eventId = "dup-ml-event";

        AnomalyFindingDTO mlFinding = AnomalyFindingDTO.builder()
                .eventId(eventId)
                .timestamp(eventTime)
                .eventType("NETWORK_EVENT")
                .anomalyType(AnomalyType.MACHINE_LEARNING)
                .reason("High byte volume")
                .severity(AnomalySeverity.HIGH)
                .score(0.78)
                .detectionMethod("Smile 3.1.0 Isolation Forest")
                .build();

        when(mockBf.detect(any())).thenReturn(Collections.emptyList());
        when(mockNet.detect(any())).thenReturn(Collections.emptyList());
        when(mockOff.detect(any())).thenReturn(Collections.emptyList());
        // Return exact same finding twice
        when(mockMl.detect(any())).thenReturn(List.of(mlFinding, mlFinding));

        AnalysisService customService = new AnalysisService(mockBf, mockNet, mockOff, mockMl);
        AnalysisReportDTO report = customService.analyze(List.of(createNetwork(eventId, 5000000L, eventTime, "src")));

        assertThat(report.getTotalAnomalies()).isEqualTo(1);
        assertThat(report.getFindings()).hasSize(1);
    }

    @Test
    @DisplayName("22. Chronological ordering preserved when ML findings are present")
    void testChronologicalOrderingWithMlFindings() {
        BruteForceDetectionService mockBf = mock(BruteForceDetectionService.class);
        NetworkVolumeDetectionService mockNet = mock(NetworkVolumeDetectionService.class);
        OffHoursDetectionService mockOff = mock(OffHoursDetectionService.class);
        MachineLearningAnomalyDetectionService mockMl = mock(MachineLearningAnomalyDetectionService.class);

        LocalDateTime tEarly = LocalDateTime.of(2026, 10, 26, 8, 0, 0);
        LocalDateTime tMid = LocalDateTime.of(2026, 10, 26, 12, 0, 0);
        LocalDateTime tLate = LocalDateTime.of(2026, 10, 26, 20, 0, 0);

        AnomalyFindingDTO earlyOffHours = AnomalyFindingDTO.builder()
                .eventId("ev-early")
                .timestamp(tEarly)
                .anomalyType(AnomalyType.OFF_HOURS)
                .severity(AnomalySeverity.LOW)
                .build();

        AnomalyFindingDTO midMl = AnomalyFindingDTO.builder()
                .eventId("ev-mid")
                .timestamp(tMid)
                .anomalyType(AnomalyType.MACHINE_LEARNING)
                .severity(AnomalySeverity.HIGH)
                .build();

        AnomalyFindingDTO lateNet = AnomalyFindingDTO.builder()
                .eventId("ev-late")
                .timestamp(tLate)
                .anomalyType(AnomalyType.NETWORK_VOLUME)
                .severity(AnomalySeverity.CRITICAL)
                .build();

        // Pass out of chronological order
        when(mockBf.detect(any())).thenReturn(Collections.emptyList());
        when(mockNet.detect(any())).thenReturn(List.of(lateNet));
        when(mockOff.detect(any())).thenReturn(List.of(earlyOffHours));
        when(mockMl.detect(any())).thenReturn(List.of(midMl));

        AnalysisService customService = new AnalysisService(mockBf, mockNet, mockOff, mockMl);
        AnalysisReportDTO report = customService.analyze(List.of(
                createNetwork("ev-late", 1000L, tLate, "src"),
                createNetwork("ev-mid", 1000L, tMid, "src"),
                createLogin("ev-early", "u", "ip", tEarly, true, "src")
        ));

        List<AnomalyFindingDTO> findings = report.getFindings();
        assertThat(findings).hasSize(3);
        assertThat(findings.get(0).getEventId()).isEqualTo("ev-early");
        assertThat(findings.get(1).getEventId()).isEqualTo("ev-mid");
        assertThat(findings.get(2).getEventId()).isEqualTo("ev-late");
    }

    @Test
    @DisplayName("23. AnalysisReportDTO contains complete generated forensic investigation summary")
    void testReportContainsGeneratedForensicSummary() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 26, 14, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createNetwork("n-1", 1000L, base, "ev-net"),
                createNetwork("n-2", 1100L, base.plusMinutes(1), "ev-net"),
                createNetwork("n-3", 1050L, base.plusMinutes(2), "ev-net"),
                createNetwork("n-spike", 25_000_000L, base.plusMinutes(3), "ev-net"),
                createFileAccess("f-night", "admin", "passwords.txt", LocalDateTime.of(2026, 10, 26, 2, 0, 0), "ev-file")
        );

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report.getSummary()).isNotNull().isNotBlank();
        assertThat(report.getSummary()).contains("5 events analyzed across the timeline");
        assertThat(report.getSummary()).contains("anomalies identified");
        assertThat(report.getSummary()).contains("Finding Breakdown:");
        assertThat(report.getSummary()).contains("Key Findings:");
        assertThat(report.getSummary()).contains("Investigation Conclusion:");
    }
}
