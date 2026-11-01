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

class MachineLearningAnomalyDetectionServiceTest {

    private MachineLearningAnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        // Default configuration: threshold 0.60, 100 trees, 0.7 subsample, seed 42, min batch size 5
        service = new MachineLearningAnomalyDetectionService(0.60, 100, 0.7, 42L, 5);
    }

    private NormalizedEventDTO createEvent(
            String id,
            String type,
            LocalDateTime timestamp,
            Long bytes,
            Integer port,
            String action,
            String description
    ) {
        return NormalizedEventDTO.builder()
                .eventId(id != null ? id : "urn:uuid:" + UUID.randomUUID())
                .caseId("case-ml-test")
                .timestamp(timestamp != null ? timestamp : LocalDateTime.of(2026, 10, 26, 11, 0, 0))
                .eventType(type != null ? type : "NETWORK_EVENT")
                .user("analyst")
                .device("WORKSTATION-01")
                .ipAddress("192.168.1.50")
                .destinationIp("10.0.0.1")
                .port(port != null ? port : 443)
                .bytes(bytes != null ? bytes : 1500L)
                .action(action != null ? action : "READ")
                .sourceEvidenceId("ev-ml-01")
                .description(description != null ? description : "Normal system activity")
                .build();
    }

    private List<NormalizedEventDTO> createStandardBaseline(int count) {
        List<NormalizedEventDTO> baseline = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            baseline.add(createEvent(
                    "base-ev-" + i,
                    "NETWORK_EVENT",
                    LocalDateTime.of(2026, 10, 26, 9 + (i % 8), i % 60, 0),
                    (long) (1000 + (i * 50)),
                    443,
                    "READ",
                    "Normal HTTPS connection"
            ));
        }
        return baseline;
    }

    @Test
    @DisplayName("1. Null input returns empty list safely")
    void testNullInputReturnsEmptyList() {
        List<AnomalyFindingDTO> findings = service.detect(null);
        assertThat(findings).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("2. Empty input returns empty list safely")
    void testEmptyInputReturnsEmptyList() {
        List<AnomalyFindingDTO> findings = service.detect(Collections.emptyList());
        assertThat(findings).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("3. Very small batch below min-batch-size returns empty list")
    void testVerySmallBatchReturnsEmptyList() {
        List<NormalizedEventDTO> events = List.of(
                createEvent("ev-1", "LOGIN", null, null, null, null, "User alice login attempt: SUCCESS"),
                createEvent("ev-2", "LOGIN", null, null, null, null, "User alice login attempt: SUCCESS"),
                createEvent("ev-3", "LOGIN", null, null, null, null, "User alice login attempt: SUCCESS")
        );

        boolean fitted = service.fit(events);
        assertThat(fitted).isFalse();
        assertThat(service.isFitted()).isFalse();

        List<AnomalyFindingDTO> findings = service.detect(events);
        assertThat(findings).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("4. Normal batch produces no false-positive anomalies")
    void testNormalBatchProducesNoAnomalies() {
        // Fit on legitimate baseline of 20 uniform daytime login events
        List<NormalizedEventDTO> baseline = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            baseline.add(createEvent(
                    "base-ev-" + i,
                    "LOGIN",
                    LocalDateTime.of(2026, 10, 26, 10, i, 0),
                    null,
                    null,
                    "AUTHENTICATE",
                    "User analyst login attempt: SUCCESS"
            ));
        }
        service.fit(baseline);

        // Score 5 normal case events
        List<NormalizedEventDTO> caseEvents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            caseEvents.add(createEvent(
                    "case-ev-" + i,
                    "LOGIN",
                    LocalDateTime.of(2026, 10, 26, 10, i + 5, 0),
                    null,
                    null,
                    "AUTHENTICATE",
                    "User analyst login attempt: SUCCESS"
            ));
        }

        List<AnomalyFindingDTO> findings = service.detect(caseEvents);
        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("5. Clearly unusual network bytes detected as anomaly")
    void testClearlyUnusualNetworkBytesDetected() {
        // 19 normal network events (1000 - 1500 bytes) as legitimate baseline
        List<NormalizedEventDTO> baseline = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            baseline.add(createEvent(
                    "norm-" + i,
                    "NETWORK_EVENT",
                    LocalDateTime.of(2026, 10, 26, 14, i, 0),
                    1200L + (i * 10),
                    443,
                    "READ",
                    "Normal HTTPS connection"
            ));
        }
        service.fit(baseline);

        // Case event: massive exfiltration event (10,000,000,000 bytes)
        NormalizedEventDTO extremeSpike = createEvent(
                "spike-999",
                "NETWORK_EVENT",
                LocalDateTime.of(2026, 10, 26, 14, 30, 0),
                10_000_000_000L,
                443,
                "READ",
                "Massive data transfer"
        );

        List<AnomalyFindingDTO> findings = service.detect(List.of(extremeSpike));

        assertThat(findings).isNotEmpty();
        AnomalyFindingDTO spikeFinding = findings.stream()
                .filter(f -> "spike-999".equals(f.getEventId()))
                .findFirst()
                .orElse(null);

        assertThat(spikeFinding).isNotNull();
        assertThat(spikeFinding.getEventId()).isEqualTo("spike-999");
        assertThat(spikeFinding.getAnomalyType()).isEqualTo(AnomalyType.MACHINE_LEARNING);
        assertThat(spikeFinding.getScore()).isGreaterThanOrEqualTo(service.getScoreThreshold());
        assertThat(spikeFinding.getDetectionMethod()).contains("Smile 3.1.0 Isolation Forest");
        assertThat(spikeFinding.getReason())
                .contains("10000000000")
                .containsIgnoringCase("data transfer");
    }

    @Test
    @DisplayName("6. Nullable bytes, port, timestamp and description handled safely")
    void testNullableFieldsHandledSafely() {
        List<NormalizedEventDTO> baseline = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            baseline.add(NormalizedEventDTO.builder()
                    .eventId("null-base-" + i)
                    .caseId("case-null")
                    .timestamp(LocalDateTime.of(2026, 10, 26, 9 + i, 0, 0))
                    .eventType("FILE_ACCESS")
                    .port(null)
                    .bytes((long) (100 * (i + 1)))
                    .action("READ")
                    .description(null)
                    .sourceEvidenceId("ev-null")
                    .build());
        }
        service.fit(baseline);

        List<NormalizedEventDTO> caseEvents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            caseEvents.add(NormalizedEventDTO.builder()
                    .eventId("null-case-" + i)
                    .caseId("case-null")
                    .timestamp(null) // null timestamp
                    .eventType("FILE_ACCESS")
                    .port(null)      // null port
                    .bytes(null)     // null bytes
                    .action(null)    // null action
                    .description(null)
                    .sourceEvidenceId("ev-null")
                    .build());
        }

        // Must execute without NullPointerException
        List<AnomalyFindingDTO> findings = service.detect(caseEvents);
        assertThat(findings).isNotNull();
    }

    @Test
    @DisplayName("7. Multiple event types in batch handled safely")
    void testMultipleEventTypesHandledSafely() {
        List<NormalizedEventDTO> baseline = createStandardBaseline(15);
        service.fit(baseline);

        List<NormalizedEventDTO> caseEvents = List.of(
                createEvent("ev-1", "LOGIN", null, null, null, null, "User a login attempt: SUCCESS"),
                createEvent("ev-2", "FILE_ACCESS", null, 500L, null, "READ", "Read file"),
                createEvent("ev-3", "NETWORK_EVENT", null, 2048L, 80, "TRANSFER", "HTTP connection"),
                createEvent("ev-4", "BROWSER_ACTIVITY", null, null, null, "VISIT", "Visited google.com"),
                createEvent("ev-5", "SYSTEM_EVENT", null, 0L, null, "START", "Service started"),
                createEvent("ev-6", "LOGIN", null, null, null, null, "User b login attempt: SUCCESS")
        );

        List<AnomalyFindingDTO> findings = service.detect(caseEvents);
        assertThat(findings).isNotNull();
    }

    @Test
    @DisplayName("8. Deterministic output for same input batch (reproducibility)")
    void testDeterministicOutput() {
        List<NormalizedEventDTO> baseline = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            baseline.add(createEvent(
                    "det-" + i,
                    "NETWORK_EVENT",
                    LocalDateTime.of(2026, 10, 26, 9 + (i % 8), 0, 0),
                    (long) (1000 + (i * 200)),
                    80 + i,
                    "READ",
                    "Traffic " + i
            ));
        }
        service.fit(baseline);

        // Case outlier
        NormalizedEventDTO outlier = createEvent(
                "det-outlier",
                "NETWORK_EVENT",
                LocalDateTime.of(2026, 10, 26, 3, 0, 0), // 3 AM off-hours
                8_000_000_000L,                          // 8 GB
                65530,                                   // high port
                "EXPORT",
                "Exfiltration"
        );

        List<AnomalyFindingDTO> run1 = service.detect(List.of(outlier));
        List<AnomalyFindingDTO> run2 = service.detect(List.of(outlier));

        assertThat(run1).hasSameSizeAs(run2);
        for (int i = 0; i < run1.size(); i++) {
            AnomalyFindingDTO f1 = run1.get(i);
            AnomalyFindingDTO f2 = run2.get(i);
            assertThat(f1.getEventId()).isEqualTo(f2.getEventId());
            assertThat(f1.getScore()).isEqualTo(f2.getScore());
            assertThat(f1.getReason()).isEqualTo(f2.getReason());
            assertThat(f1.getSeverity()).isEqualTo(f2.getSeverity());
            assertThat(f1.getDetectionMethod()).isEqualTo(f2.getDetectionMethod());
        }
    }

    @Test
    @DisplayName("9. Preserves original M2 event_id in every ML finding")
    void testEventIdPreservation() {
        List<NormalizedEventDTO> baseline = createStandardBaseline(15);
        service.fit(baseline);

        String customId = "forensic-evidence-target-xyz-987";
        NormalizedEventDTO outlier = createEvent(
                customId,
                "NETWORK_EVENT",
                LocalDateTime.of(2026, 10, 26, 2, 15, 0),
                9_999_999_999L,
                4444,
                "EXPORT",
                "Suspicious transfer"
        );

        List<AnomalyFindingDTO> findings = service.detect(List.of(outlier));
        assertThat(findings).anyMatch(f -> customId.equals(f.getEventId()));
    }

    @Test
    @DisplayName("10. Does not mutate the input event list")
    void testNoInputMutation() {
        List<NormalizedEventDTO> baseline = createStandardBaseline(10);
        List<NormalizedEventDTO> baselineCopy = new ArrayList<>(baseline);
        service.fit(baseline);
        assertThat(baseline).containsExactlyElementsOf(baselineCopy);

        List<NormalizedEventDTO> caseList = List.of(
                createEvent("case-1", "LOGIN", null, null, null, null, "login: SUCCESS"),
                createEvent("case-2", "NETWORK_EVENT", null, 5000L, 80, "READ", "traffic")
        );
        List<NormalizedEventDTO> caseCopy = new ArrayList<>(caseList);

        service.detect(caseList);
        assertThat(caseList).containsExactlyElementsOf(caseCopy);
    }

    @Test
    @DisplayName("11. Validates score in [0.0, 1.0] and valid severity assignment")
    void testScoreAndSeverityValidity() {
        List<NormalizedEventDTO> baseline = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            baseline.add(createEvent("ev-" + i, "FILE_ACCESS",
                    LocalDateTime.of(2026, 10, 26, 9 + (i % 8), 0, 0),
                    100L + (i * 10), null, "READ", "read doc"));
        }
        service.fit(baseline);

        NormalizedEventDTO outlier = createEvent("outlier-1", "FILE_ACCESS",
                LocalDateTime.of(2026, 10, 26, 1, 0, 0),
                50_000_000L, null, "DELETE", "mass delete");

        List<AnomalyFindingDTO> findings = service.detect(List.of(outlier));

        for (AnomalyFindingDTO finding : findings) {
            assertThat(finding.getScore()).isNotNull();
            assertThat(finding.getScore()).isBetween(0.0, 1.0);
            assertThat(finding.getSeverity()).isNotNull();
            assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.MACHINE_LEARNING);
        }
    }

    @Test
    @DisplayName("12. No duplicate findings for the same event")
    void testNoDuplicateFindingsForSameEvent() {
        List<NormalizedEventDTO> baseline = createStandardBaseline(15);
        service.fit(baseline);

        NormalizedEventDTO outlier = createEvent("dup-id-1", "NETWORK_EVENT", null, 5_000_000_000L, 9999, "EXPORT", "Spike");

        List<AnomalyFindingDTO> findings = service.detect(List.of(outlier, outlier));

        long count = findings.stream().filter(f -> "dup-id-1".equals(f.getEventId())).count();
        assertThat(count).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("13. Custom configuration settings are respected")
    void testCustomConfigurationRespected() {
        MachineLearningAnomalyDetectionService customService =
                new MachineLearningAnomalyDetectionService(0.85, 50, 0.5, 999L, 10);

        assertThat(customService.getScoreThreshold()).isEqualTo(0.85);
        assertThat(customService.getNumTrees()).isEqualTo(50);
        assertThat(customService.getSubsample()).isEqualTo(0.5);
        assertThat(customService.getRandomSeed()).isEqualTo(999L);
        assertThat(customService.getMinBatchSize()).isEqualTo(10);

        // Baseline with 7 items (< 10) should return empty list and not fit
        List<NormalizedEventDTO> events = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            events.add(createEvent("ev-" + i, "LOGIN", null, null, null, null, "login: SUCCESS"));
        }
        assertThat(customService.fit(events)).isFalse();
        assertThat(customService.detect(events)).isEmpty();
    }

    @Test
    @DisplayName("14. Baseline events are used for model fitting and case events are scored separately")
    void testBaselineEventsUsedForModelFittingAndCaseEventsScoredSeparately() {
        List<NormalizedEventDTO> baseline = createStandardBaseline(20);
        boolean fitted = service.fit(baseline);
        assertThat(fitted).isTrue();
        assertThat(service.isFitted()).isTrue();

        NormalizedEventDTO caseEvent = createEvent("case-anom-1", "NETWORK_EVENT",
                LocalDateTime.of(2026, 10, 26, 2, 0, 0),
                9_000_000_000L, 4444, "EXPORT", "Exfiltration");

        List<AnomalyFindingDTO> findings = service.detect(List.of(caseEvent));

        // Finding corresponds strictly to the case event, not baseline events
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getEventId()).isEqualTo("case-anom-1");
    }

    @Test
    @DisplayName("15. Case events are NOT added to the training baseline")
    void testCaseEventsAreNotAddedToTrainingBaseline() {
        InMemoryHistoricalBaselineProvider provider = new InMemoryHistoricalBaselineProvider();
        List<NormalizedEventDTO> initialBaseline = createStandardBaseline(15);
        provider.setBaselineEvents(initialBaseline);

        service.setBaselineProvider(provider);
        service.fit(provider.getBaselineEvents());

        int baselineCountBefore = provider.getBaselineEvents().size();
        assertThat(baselineCountBefore).isEqualTo(15);

        NormalizedEventDTO caseEvent1 = createEvent("case-spike-1", "NETWORK_EVENT", null, 10_000_000_000L, 8888, "EXPORT", "Attack");
        NormalizedEventDTO caseEvent2 = createEvent("case-spike-2", "NETWORK_EVENT", null, 12_000_000_000L, 8888, "EXPORT", "Attack");

        service.detect(List.of(caseEvent1, caseEvent2));

        // Provider still has exact same count and contents
        assertThat(provider.getBaselineEvents()).hasSize(baselineCountBefore);
        assertThat(provider.getBaselineEvents()).extracting(NormalizedEventDTO::getEventId)
                .doesNotContain("case-spike-1", "case-spike-2");
    }

    @Test
    @DisplayName("16. Unfitted model safely returns empty findings without exception")
    void testUnfittedModelReturnsEmptyFindingsSafely() {
        service.resetModel();
        assertThat(service.isFitted()).isFalse();

        List<NormalizedEventDTO> caseEvents = List.of(
                createEvent("case-1", "NETWORK_EVENT", null, 9_000_000_000L, 4444, "EXPORT", "Suspicious")
        );

        List<AnomalyFindingDTO> findings = service.detect(caseEvents);
        assertThat(findings).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("17. Attack-dominant case batch is correctly flagged when fitted on normal baseline")
    void testAttackDominantCaseBatchIsCorrectlyFlaggedWhenFittedOnNormalBaseline() {
        // Legitimate baseline representing typical business HTTPS traffic
        List<NormalizedEventDTO> normalBaseline = createStandardBaseline(25);
        service.fit(normalBaseline);

        // Case batch contains 100% attack events (5 massive exfiltrations)
        List<NormalizedEventDTO> attackCase = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            attackCase.add(createEvent(
                    "attack-" + i,
                    "NETWORK_EVENT",
                    LocalDateTime.of(2026, 10, 26, 2, i, 0),
                    8_000_000_000L + (i * 100_000_000L),
                    4444 + i,
                    "EXPORT",
                    "Data exfiltration incident"
            ));
        }

        List<AnomalyFindingDTO> findings = service.detect(attackCase);

        // All 5 attack events should be successfully identified because the baseline defines normality
        assertThat(findings).isNotEmpty();
        assertThat(findings).extracting(AnomalyFindingDTO::getEventId)
                .contains("attack-0", "attack-1", "attack-2", "attack-3", "attack-4");
    }
}
