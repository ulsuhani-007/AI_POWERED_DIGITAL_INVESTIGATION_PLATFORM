package com.cybertrace.m3.service;

import com.cybertrace.m3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class InvestigationSummaryServiceTest {

    private InvestigationSummaryService summaryService;

    @BeforeEach
    void setUp() {
        summaryService = new InvestigationSummaryService();
    }

    private AnomalyFindingDTO createFinding(
            String eventId,
            LocalDateTime timestamp,
            AnomalyType anomalyType,
            AnomalySeverity severity,
            Double score,
            String reason
    ) {
        return AnomalyFindingDTO.builder()
                .eventId(eventId)
                .timestamp(timestamp)
                .eventType("NETWORK_EVENT")
                .anomalyType(anomalyType)
                .severity(severity)
                .score(score)
                .reason(reason)
                .detectionMethod("TEST_DETECTOR")
                .build();
    }

    private AnalysisReportDTO createReport(int totalEvents, List<AnomalyFindingDTO> findings) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_events", totalEvents);

        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<String, Integer> bySev = new LinkedHashMap<>();

        if (findings != null) {
            for (AnomalyFindingDTO f : findings) {
                String t = f.getAnomalyType() != null ? f.getAnomalyType().name() : "UNKNOWN";
                byType.put(t, byType.getOrDefault(t, 0) + 1);

                String s = f.getSeverity() != null ? f.getSeverity().name() : "UNKNOWN";
                bySev.put(s, bySev.getOrDefault(s, 0) + 1);
            }
        }

        stats.put("findings_by_anomaly_type", byType);
        stats.put("findings_by_severity", bySev);

        return AnalysisReportDTO.builder()
                .totalEvents(totalEvents)
                .totalAnomalies(findings != null ? findings.size() : 0)
                .findings(findings)
                .statistics(stats)
                .summary("")
                .build();
    }

    @Test
    @DisplayName("1. Null report returns safe summary")
    void testNullReportReturnsSafeSummary() {
        String summary = summaryService.generateSummary(null);

        assertThat(summary).isNotNull();
        assertThat(summary).contains("0 events analyzed");
        assertThat(summary).contains("0 anomalies identified");
        assertThat(summary).contains("No anomalies detected");
    }

    @Test
    @DisplayName("2. Zero-event report returns safe summary with clear conclusion")
    void testZeroEventReportReturnsSafeSummary() {
        AnalysisReportDTO report = createReport(0, Collections.emptyList());

        String summary = summaryService.generateSummary(report);

        assertThat(summary).isNotNull();
        assertThat(summary).contains("0 events analyzed");
        assertThat(summary).contains("0 anomalies identified");
        assertThat(summary).contains("No anomalies detected");
    }

    @Test
    @DisplayName("3. Normal report with zero anomalies reports baseline compliance")
    void testNormalReportWithZeroAnomalies() {
        AnalysisReportDTO report = createReport(45, Collections.emptyList());

        String summary = summaryService.generateSummary(report);

        assertThat(summary).contains("45 events analyzed across the timeline; 0 anomalies identified.");
        assertThat(summary).contains("No anomalies detected. Analyzed events conform to expected baseline patterns.");
        assertThat(summary).doesNotContain("Finding Breakdown");
        assertThat(summary).doesNotContain("Key Findings");
    }

    @Test
    @DisplayName("4. Single anomaly report formats properly with singular wording")
    void testSingleAnomalyReport() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 26, 14, 30, 0);
        AnomalyFindingDTO finding = createFinding(
                "ev-net-01",
                time,
                AnomalyType.NETWORK_VOLUME,
                AnomalySeverity.HIGH,
                0.87,
                "Transfer volume of 50000000 bytes exceeds threshold"
        );

        AnalysisReportDTO report = createReport(10, List.of(finding));

        String summary = summaryService.generateSummary(report);

        assertThat(summary).contains("10 events analyzed across the timeline; 1 anomaly identified.");
        assertThat(summary).contains("- Anomaly Types: NETWORK_VOLUME (1)");
        assertThat(summary).contains("- Severity Distribution: HIGH (1)");
        assertThat(summary).contains("[HIGH] NETWORK_VOLUME on event 'ev-net-01' at 2026-10-26T14:30:00: Transfer volume of 50000000 bytes exceeds threshold");
        assertThat(summary).contains("Suspicious activity identified requiring analyst review.");
    }

    @Test
    @DisplayName("5. Multiple anomaly types are categorized and counted alphabetically")
    void testMultipleAnomalyTypes() {
        List<AnomalyFindingDTO> findings = List.of(
                createFinding("ev-1", LocalDateTime.now(), AnomalyType.OFF_HOURS, AnomalySeverity.MEDIUM, null, "Off-hours login"),
                createFinding("ev-2", LocalDateTime.now(), AnomalyType.BRUTE_FORCE, AnomalySeverity.HIGH, null, "Brute-force attack detected"),
                createFinding("ev-3", LocalDateTime.now(), AnomalyType.MACHINE_LEARNING, AnomalySeverity.CRITICAL, 0.91, "Outlier score")
        );

        AnalysisReportDTO report = createReport(25, findings);
        String summary = summaryService.generateSummary(report);

        assertThat(summary).contains("BRUTE_FORCE (1)");
        assertThat(summary).contains("MACHINE_LEARNING (1)");
        assertThat(summary).contains("OFF_HOURS (1)");
    }

    @Test
    @DisplayName("6. Multiple severities are ordered by precedence (CRITICAL -> HIGH -> MEDIUM -> LOW)")
    void testMultipleSeveritiesOrderedByPrecedence() {
        List<AnomalyFindingDTO> findings = List.of(
                createFinding("ev-1", LocalDateTime.now(), AnomalyType.OFF_HOURS, AnomalySeverity.LOW, null, "Low finding"),
                createFinding("ev-2", LocalDateTime.now(), AnomalyType.OFF_HOURS, AnomalySeverity.CRITICAL, null, "Critical finding"),
                createFinding("ev-3", LocalDateTime.now(), AnomalyType.OFF_HOURS, AnomalySeverity.MEDIUM, null, "Medium finding"),
                createFinding("ev-4", LocalDateTime.now(), AnomalyType.OFF_HOURS, AnomalySeverity.HIGH, null, "High finding")
        );

        AnalysisReportDTO report = createReport(30, findings);
        String summary = summaryService.generateSummary(report);

        int idxCrit = summary.indexOf("CRITICAL (1)");
        int idxHigh = summary.indexOf("HIGH (1)");
        int idxMed = summary.indexOf("MEDIUM (1)");
        int idxLow = summary.indexOf("LOW (1)");

        assertThat(idxCrit).isLessThan(idxHigh);
        assertThat(idxHigh).isLessThan(idxMed);
        assertThat(idxMed).isLessThan(idxLow);
    }

    @Test
    @DisplayName("7. Highest-severity finding selection focuses Key Findings on top severity only")
    void testHighestSeverityFindingSelection() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 26, 10, 5, 0);

        List<AnomalyFindingDTO> findings = List.of(
                createFinding("ev-med", t1, AnomalyType.OFF_HOURS, AnomalySeverity.MEDIUM, null, "Medium issue"),
                createFinding("ev-crit", t2, AnomalyType.NETWORK_VOLUME, AnomalySeverity.CRITICAL, null, "Critical exfiltration")
        );

        AnalysisReportDTO report = createReport(20, findings);
        String summary = summaryService.generateSummary(report);

        // Key Findings should highlight ev-crit but omit ev-med
        assertThat(summary).contains("[CRITICAL] NETWORK_VOLUME on event 'ev-crit'");
        assertThat(summary).doesNotContain("[MEDIUM]");
    }

    @Test
    @DisplayName("8. Machine Learning finding includes ML Score in summary")
    void testMachineLearningFindingWithScore() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 26, 15, 0, 0);
        AnomalyFindingDTO mlFinding = createFinding(
                "ml-target-99",
                time,
                AnomalyType.MACHINE_LEARNING,
                AnomalySeverity.HIGH,
                0.7854,
                "Machine Learning anomaly detected (score: 0.785): unusual byte volume"
        );

        AnalysisReportDTO report = createReport(18, List.of(mlFinding));
        String summary = summaryService.generateSummary(report);

        assertThat(summary).contains("ML Score: 0.7854");
        assertThat(summary).contains("ml-target-99");
    }

    @Test
    @DisplayName("9. Missing or null optional fields handled safely without exceptions")
    void testMissingOptionalFieldsHandledSafely() {
        AnomalyFindingDTO incompleteFinding = AnomalyFindingDTO.builder()
                .eventId(null)          // null eventId
                .timestamp(null)        // null timestamp
                .eventType(null)
                .anomalyType(null)      // null anomalyType
                .severity(null)         // null severity
                .score(null)            // null score
                .reason(null)           // null reason
                .detectionMethod(null)
                .build();

        AnalysisReportDTO report = AnalysisReportDTO.builder()
                .totalEvents(5)
                .totalAnomalies(1)
                .findings(List.of(incompleteFinding))
                .statistics(null)       // null statistics map
                .summary("")
                .build();

        String summary = summaryService.generateSummary(report);

        assertThat(summary).isNotNull();
        assertThat(summary).contains("5 events analyzed");
        assertThat(summary).contains("1 anomaly identified");
        assertThat(summary).contains("Anomalous forensic pattern identified.");
    }

    @Test
    @DisplayName("10. Deterministic repeated output: same input produces identical summary")
    void testDeterministicRepeatedOutput() {
        List<AnomalyFindingDTO> findings = List.of(
                createFinding("ev-1", LocalDateTime.of(2026, 10, 26, 1, 0, 0), AnomalyType.OFF_HOURS, AnomalySeverity.HIGH, null, "Off hours"),
                createFinding("ev-2", LocalDateTime.of(2026, 10, 26, 2, 0, 0), AnomalyType.MACHINE_LEARNING, AnomalySeverity.HIGH, 0.75, "ML outlier")
        );
        AnalysisReportDTO report = createReport(15, findings);

        String run1 = summaryService.generateSummary(report);
        String run2 = summaryService.generateSummary(report);

        assertThat(run1).isEqualTo(run2);
    }

    @Test
    @DisplayName("11. Event ID preservation in summary")
    void testEventIdPreservationInSummary() {
        String exactId = "forensic-evidence-target-uuid-4444";
        AnomalyFindingDTO finding = createFinding(
                exactId,
                LocalDateTime.of(2026, 10, 26, 12, 0, 0),
                AnomalyType.NETWORK_VOLUME,
                AnomalySeverity.CRITICAL,
                null,
                "Massive byte transfer"
        );
        AnalysisReportDTO report = createReport(8, List.of(finding));

        String summary = summaryService.generateSummary(report);

        assertThat(summary).contains(exactId);
    }

    @Test
    @DisplayName("12. No invented facts: conclusion is strictly objective and neutral")
    void testNoInventedFacts() {
        AnomalyFindingDTO finding = createFinding(
                "ev-test",
                LocalDateTime.of(2026, 10, 26, 14, 0, 0),
                AnomalyType.OFF_HOURS,
                AnomalySeverity.MEDIUM,
                null,
                "Activity at 14:00"
        );
        AnalysisReportDTO report = createReport(10, List.of(finding));

        String summary = summaryService.generateSummary(report);

        // Ensures no hallucinated attacker, malicious intent, or external entities
        assertThat(summary).doesNotContainIgnoringCase("hacker");
        assertThat(summary).doesNotContainIgnoringCase("malicious actor");
        assertThat(summary).doesNotContainIgnoringCase("breach confirmed");
        assertThat(summary).doesNotContainIgnoringCase("compromised");
        assertThat(summary).contains("Suspicious activity identified requiring analyst review.");
    }

    @Test
    @DisplayName("13. Empty findings list safely outputs no anomalies detected")
    void testEmptyFindingsSafelyOutputsNoAnomalies() {
        AnalysisReportDTO report = AnalysisReportDTO.builder()
                .totalEvents(12)
                .totalAnomalies(0)
                .findings(Collections.emptyList())
                .statistics(Collections.emptyMap())
                .summary("")
                .build();

        String summary = summaryService.generateSummary(report);

        assertThat(summary).contains("12 events analyzed");
        assertThat(summary).contains("0 anomalies identified");
        assertThat(summary).contains("No anomalies detected");
    }

    @Test
    @DisplayName("14. Multiple findings with same highest severity are all presented chronologically")
    void testMultipleFindingsWithSameSeverity() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 26, 11, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 10, 26, 12, 0, 0);

        List<AnomalyFindingDTO> findings = List.of(
                createFinding("ev-crit-2", t2, AnomalyType.OFF_HOURS, AnomalySeverity.CRITICAL, null, "Critical 2"),
                createFinding("ev-crit-1", t1, AnomalyType.BRUTE_FORCE, AnomalySeverity.CRITICAL, null, "Critical 1"),
                createFinding("ev-crit-3", t3, AnomalyType.NETWORK_VOLUME, AnomalySeverity.CRITICAL, null, "Critical 3")
        );

        AnalysisReportDTO report = createReport(30, findings);
        String summary = summaryService.generateSummary(report);

        // All 3 critical findings must be present in chronological order
        int idx1 = summary.indexOf("ev-crit-1");
        int idx2 = summary.indexOf("ev-crit-2");
        int idx3 = summary.indexOf("ev-crit-3");

        assertThat(idx1).isGreaterThan(0);
        assertThat(idx2).isGreaterThan(idx1);
        assertThat(idx3).isGreaterThan(idx2);
    }
}
