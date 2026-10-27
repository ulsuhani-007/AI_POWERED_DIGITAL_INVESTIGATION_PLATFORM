package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OffHoursDetectionServiceTest {

    private OffHoursDetectionService service;

    @BeforeEach
    void setUp() {
        // Default working hours: 09:00 through 18:00
        service = new OffHoursDetectionService(9, 18);
    }

    private NormalizedEventDTO createEvent(String eventType, LocalDateTime timestamp, String id) {
        return NormalizedEventDTO.builder()
                .eventId(id != null ? id : "urn:uuid:" + UUID.randomUUID())
                .caseId("case-test")
                .timestamp(timestamp)
                .eventType(eventType)
                .user("test-user")
                .device("test-device")
                .sourceEvidenceId("ev-src-01")
                .description("Test event description")
                .build();
    }

    @Test
    @DisplayName("1. LOGIN at 02:00 -> finding detected")
    void testLoginAt0200ProducesFinding() {
        LocalDateTime time0200 = LocalDateTime.of(2026, 10, 26, 2, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("LOGIN", time0200, "login-0200")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        AnomalyFindingDTO finding = findings.get(0);

        assertThat(finding.getEventId()).isEqualTo("login-0200");
        assertThat(finding.getEventType()).isEqualTo("LOGIN");
        assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.OFF_HOURS);
        assertThat(finding.getTimestamp()).isEqualTo(time0200);
        assertThat(finding.getDetectionMethod()).isEqualTo("RULE_BASED_OFF_HOURS");
        assertThat(finding.getSeverity()).isEqualTo(AnomalySeverity.HIGH);
        assertThat(finding.getScore()).isGreaterThanOrEqualTo(0.70);
        assertThat(finding.getReason()).contains("LOGIN activity occurred at 02:00, outside configured working hours of 09:00-18:00.");
    }

    @Test
    @DisplayName("2. LOGIN at 10:00 -> no finding (within working hours)")
    void testLoginAt1000ProducesNoFinding() {
        LocalDateTime time1000 = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("LOGIN", time1000, "login-1000")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("3. FILE_ACCESS at 03:00 -> finding detected")
    void testFileAccessAt0300ProducesFinding() {
        LocalDateTime time0300 = LocalDateTime.of(2026, 10, 26, 3, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("FILE_ACCESS", time0300, "file-0300")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        AnomalyFindingDTO finding = findings.get(0);

        assertThat(finding.getEventId()).isEqualTo("file-0300");
        assertThat(finding.getEventType()).isEqualTo("FILE_ACCESS");
        assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.OFF_HOURS);
        assertThat(finding.getReason()).contains("FILE_ACCESS activity occurred at 03:00, outside configured working hours of 09:00-18:00.");
    }

    @Test
    @DisplayName("4. FILE_ACCESS at 14:00 -> no finding (within working hours)")
    void testFileAccessAt1400ProducesNoFinding() {
        LocalDateTime time1400 = LocalDateTime.of(2026, 10, 26, 14, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("FILE_ACCESS", time1400, "file-1400")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("5. BROWSER_ACTIVITY at 02:00 is ignored")
    void testBrowserActivityAt0200Ignored() {
        LocalDateTime time0200 = LocalDateTime.of(2026, 10, 26, 2, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("BROWSER_ACTIVITY", time0200, "browser-0200")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("6. NETWORK_EVENT at 02:00 is ignored")
    void testNetworkEventAt0200Ignored() {
        LocalDateTime time0200 = LocalDateTime.of(2026, 10, 26, 2, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("NETWORK_EVENT", time0200, "net-0200")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("7. Exactly at working-hour start (09:00:00) -> normal, no finding")
    void testExactlyAtWorkingHourStartIsNormal() {
        LocalDateTime time0900 = LocalDateTime.of(2026, 10, 26, 9, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createEvent("LOGIN", time0900, "login-0900")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("8. Exactly at working-hour end (18:00:00) is normal; 18:01:00 is off-hours")
    void testWorkingHourEndBoundary() {
        LocalDateTime time1800 = LocalDateTime.of(2026, 10, 26, 18, 0, 0);
        LocalDateTime time1801 = LocalDateTime.of(2026, 10, 26, 18, 1, 0);

        // 18:00:00 is the final second of normal working hours -> not flagged
        assertThat(service.detect(List.of(createEvent("LOGIN", time1800, "login-1800")))).isEmpty();

        // 18:01:00 is past the end boundary -> flagged as off-hours (slight overtime -> LOW)
        List<AnomalyFindingDTO> findings = service.detect(List.of(createEvent("LOGIN", time1801, "login-1801")));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(AnomalySeverity.LOW);
    }

    @Test
    @DisplayName("9. Null timestamp or null event type -> safely ignored")
    void testNullTimestampOrEventTypeHandledSafely() {
        NormalizedEventDTO nullTsEvent = createEvent("LOGIN", null, "null-ts");
        NormalizedEventDTO nullTypeEvent = createEvent(null, LocalDateTime.of(2026, 10, 26, 2, 0, 0), "null-type");
        NormalizedEventDTO nullEvent = null;

        List<NormalizedEventDTO> events = List.of(nullTsEvent, nullTypeEvent);

        List<AnomalyFindingDTO> findings = service.detect(events);
        assertThat(findings).isEmpty();

        assertThat(service.detect(Collections.singletonList(nullEvent))).isEmpty();
    }

    @Test
    @DisplayName("10. Empty input or null input -> returns empty list")
    void testEmptyAndNullInputSafe() {
        assertThat(service.detect(Collections.emptyList())).isEmpty();
        assertThat(service.detect(null)).isEmpty();
    }

    @Test
    @DisplayName("11. Unsorted events -> correctly evaluated and original list unmutated")
    void testUnsortedEventsEvaluatedCorrectly() {
        List<NormalizedEventDTO> events = List.of(
                createEvent("LOGIN", LocalDateTime.of(2026, 10, 26, 14, 0, 0), "ev-normal-1"),
                createEvent("LOGIN", LocalDateTime.of(2026, 10, 26, 1, 30, 0), "ev-off-1"),
                createEvent("FILE_ACCESS", LocalDateTime.of(2026, 10, 26, 11, 0, 0), "ev-normal-2"),
                createEvent("FILE_ACCESS", LocalDateTime.of(2026, 10, 26, 23, 15, 0), "ev-off-2")
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(AnomalyFindingDTO::getEventId)
                .containsExactly("ev-off-1", "ev-off-2");
    }

    @Test
    @DisplayName("12. Event ID is strictly preserved from M2")
    void testEventIdPreserved() {
        String originalId = "urn:uuid:12345678-abcd-ef01-2345-6789abcdef01";
        LocalDateTime offTime = LocalDateTime.of(2026, 10, 26, 4, 15, 0);

        List<AnomalyFindingDTO> findings = service.detect(List.of(createEvent("LOGIN", offTime, originalId)));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getEventId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("13. Severity and scores are within valid ranges and never CRITICAL solely for off-hours")
    void testSeverityAndScoresWithinValidRanges() {
        LocalDateTime time1900 = LocalDateTime.of(2026, 10, 26, 19, 0, 0); // 1 hour outside -> LOW
        LocalDateTime time2200 = LocalDateTime.of(2026, 10, 26, 22, 0, 0); // 4 hours outside -> MEDIUM
        LocalDateTime time0200 = LocalDateTime.of(2026, 10, 26, 2, 0, 0);  // 7 hours outside -> HIGH

        List<AnomalyFindingDTO> findings = service.detect(List.of(
                createEvent("LOGIN", time1900, "ev-low"),
                createEvent("LOGIN", time2200, "ev-med"),
                createEvent("LOGIN", time0200, "ev-high")
        ));

        assertThat(findings).hasSize(3);

        AnomalyFindingDTO lowFinding = findings.get(0);
        assertThat(lowFinding.getSeverity()).isEqualTo(AnomalySeverity.LOW);
        assertThat(lowFinding.getScore()).isBetween(0.30, 0.50);

        AnomalyFindingDTO medFinding = findings.get(1);
        assertThat(medFinding.getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);
        assertThat(medFinding.getScore()).isBetween(0.50, 0.70);

        AnomalyFindingDTO highFinding = findings.get(2);
        assertThat(highFinding.getSeverity()).isEqualTo(AnomalySeverity.HIGH);
        assertThat(highFinding.getScore()).isBetween(0.70, 0.85);

        // Verify none of them are CRITICAL
        assertThat(findings).noneMatch(f -> f.getSeverity() == AnomalySeverity.CRITICAL);
    }

    @Test
    @DisplayName("14. Events just before working hours (08:59:00) -> correctly classified as off-hours")
    void testJustBeforeWorkingHoursIsOffHours() {
        LocalDateTime time0859 = LocalDateTime.of(2026, 10, 26, 8, 59, 0);
        List<AnomalyFindingDTO> findings = service.detect(List.of(createEvent("LOGIN", time0859, "login-0859")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo(AnomalySeverity.LOW);
    }

    @Test
    @DisplayName("15. Cross-midnight working interval (22:00 - 06:00) is properly supported")
    void testCrossMidnightWorkingHoursSupported() {
        OffHoursDetectionService nightShiftService = new OffHoursDetectionService(22, 6);

        // 23:30 is within night shift -> normal
        assertThat(nightShiftService.detect(List.of(
                createEvent("LOGIN", LocalDateTime.of(2026, 10, 26, 23, 30, 0), "night-1")
        ))).isEmpty();

        // 03:00 is within night shift -> normal
        assertThat(nightShiftService.detect(List.of(
                createEvent("LOGIN", LocalDateTime.of(2026, 10, 26, 3, 0, 0), "night-2")
        ))).isEmpty();

        // 14:00 is outside night shift -> off-hours
        List<AnomalyFindingDTO> findings = nightShiftService.detect(List.of(
                createEvent("LOGIN", LocalDateTime.of(2026, 10, 26, 14, 0, 0), "day-outside")
        ));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getEventId()).isEqualTo("day-outside");
    }
}
