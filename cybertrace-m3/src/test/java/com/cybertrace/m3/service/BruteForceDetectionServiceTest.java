package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BruteForceDetectionServiceTest {

    private BruteForceDetectionService service;

    @BeforeEach
    void setUp() {
        // Standard default service: threshold = 5, window = 5 minutes
        service = new BruteForceDetectionService(5, Duration.ofMinutes(5));
    }

    private NormalizedEventDTO createLoginEvent(
            String id,
            String user,
            String ip,
            LocalDateTime timestamp,
            boolean success
    ) {
        String status = success ? "SUCCESS" : "FAILED";
        return NormalizedEventDTO.builder()
                .eventId(id != null ? id : "urn:uuid:" + UUID.randomUUID())
                .caseId("case-test")
                .timestamp(timestamp)
                .eventType("LOGIN")
                .user(user)
                .device("device-1")
                .ipAddress(ip)
                .sourceEvidenceId("ev-login-01")
                .description("User " + user + " login attempt: " + status)
                .build();
    }

    @Test
    @DisplayName("1. 5 failed logins from same user/IP within 5 minutes -> finding detected")
    void testFiveFailedLoginsWithinWindowDetectsFinding() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", "attacker", "198.51.100.42", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", "attacker", "198.51.100.42", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-3", "attacker", "198.51.100.42", baseTime.plusMinutes(2), false),
                createLoginEvent("ev-4", "attacker", "198.51.100.42", baseTime.plusMinutes(3), false),
                createLoginEvent("ev-5", "attacker", "198.51.100.42", baseTime.plusMinutes(4), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        AnomalyFindingDTO finding = findings.get(0);

        assertThat(finding.getEventId()).isEqualTo("ev-5");
        assertThat(finding.getTimestamp()).isEqualTo(baseTime.plusMinutes(4));
        assertThat(finding.getEventType()).isEqualTo("LOGIN");
        assertThat(finding.getAnomalyType()).isEqualTo(AnomalyType.BRUTE_FORCE);
        assertThat(finding.getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);
        assertThat(finding.getScore()).isEqualTo(0.70);
        assertThat(finding.getDetectionMethod()).isEqualTo("RULE_BASED_TIME_WINDOW");
        assertThat(finding.getReason())
                .contains("attacker")
                .contains("198.51.100.42")
                .contains("5 failed login attempts")
                .contains("5 minutes");
    }

    @Test
    @DisplayName("2. 4 failed logins -> no finding (below threshold of 5)")
    void testFourFailedLoginsYieldsNoFinding() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", "user1", "10.0.0.5", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", "user1", "10.0.0.5", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-3", "user1", "10.0.0.5", baseTime.plusMinutes(2), false),
                createLoginEvent("ev-4", "user1", "10.0.0.5", baseTime.plusMinutes(3), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("3. 5 failed logins spread beyond the time window -> no finding")
    void testFiveFailedLoginsSpreadBeyondWindowYieldsNoFinding() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        // Events spaced 2 minutes apart (total 8 minutes span: at no point are 5 events within 5 mins)
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", "user1", "10.0.0.5", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", "user1", "10.0.0.5", baseTime.plusMinutes(2), false),
                createLoginEvent("ev-3", "user1", "10.0.0.5", baseTime.plusMinutes(4), false),
                createLoginEvent("ev-4", "user1", "10.0.0.5", baseTime.plusMinutes(6), false),
                createLoginEvent("ev-5", "user1", "10.0.0.5", baseTime.plusMinutes(8), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("4. Successful login does not count as a failed attempt")
    void testSuccessfulLoginDoesNotCountTowardsThreshold() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        // 4 failed logins and 1 successful login within 4 minutes
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", "user1", "10.0.0.5", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", "user1", "10.0.0.5", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-3", "user1", "10.0.0.5", baseTime.plusMinutes(2), true), // SUCCESS!
                createLoginEvent("ev-4", "user1", "10.0.0.5", baseTime.plusMinutes(3), false),
                createLoginEvent("ev-5", "user1", "10.0.0.5", baseTime.plusMinutes(4), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        // Only 4 failed logins exist, so threshold 5 is not reached
        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("5. Different users are handled separately")
    void testDifferentUsersHandledSeparately() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        // 3 failed logins for alice, 3 for bob from same IP
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", "alice", "10.0.0.1", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", "bob", "10.0.0.1", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-3", "alice", "10.0.0.1", baseTime.plusMinutes(2), false),
                createLoginEvent("ev-4", "bob", "10.0.0.1", baseTime.plusMinutes(3), false),
                createLoginEvent("ev-5", "alice", "10.0.0.1", baseTime.plusMinutes(4), false),
                createLoginEvent("ev-6", "bob", "10.0.0.1", baseTime.plusMinutes(4), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        // Neither user reaches 5 failed attempts
        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("6. Different IP addresses are handled separately")
    void testDifferentIpAddressesHandledSeparately() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        // 3 failed logins from IP1, 3 from IP2 for the same user
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", "alice", "192.168.1.10", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", "alice", "10.0.0.5", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-3", "alice", "192.168.1.10", baseTime.plusMinutes(2), false),
                createLoginEvent("ev-4", "alice", "10.0.0.5", baseTime.plusMinutes(3), false),
                createLoginEvent("ev-5", "alice", "192.168.1.10", baseTime.plusMinutes(4), false),
                createLoginEvent("ev-6", "alice", "10.0.0.5", baseTime.plusMinutes(4), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        // Neither IP group reaches 5 failed attempts
        assertThat(findings).isEmpty();
    }

    @Test
    @DisplayName("7. Null user/IP is handled safely")
    void testNullUserAndIpHandledSafely() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-1", null, "192.168.1.100", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-2", null, "192.168.1.100", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-3", null, "192.168.1.100", baseTime.plusMinutes(2), false),
                createLoginEvent("ev-4", null, "192.168.1.100", baseTime.plusMinutes(3), false),
                createLoginEvent("ev-5", null, "192.168.1.100", baseTime.plusMinutes(4), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getReason())
                .contains("<UNKNOWN_USER>")
                .contains("192.168.1.100");
    }

    @Test
    @DisplayName("8. Duplicate findings are not produced for one continuous incident")
    void testDuplicateFindingsNotProducedForSingleIncident() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        // 7 failed attempts in rapid succession (within 3 minutes)
        List<NormalizedEventDTO> events = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            events.add(createLoginEvent("ev-" + i, "attacker", "198.51.100.42", baseTime.plusSeconds(i * 30), false));
        }

        List<AnomalyFindingDTO> findings = service.detect(events);

        // Must produce exactly ONE finding for this burst, not 3 overlapping findings
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getReason()).contains("7 failed login attempts");
        assertThat(findings.get(0).getEventId()).isEqualTo("ev-6");
    }

    @Test
    @DisplayName("9. Event IDs in findings match the original M2 event IDs")
    void testEventIdsMatchOriginalM2EventIds() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        String id1 = "urn:uuid:aaaa-1111";
        String id2 = "urn:uuid:bbbb-2222";
        String id3 = "urn:uuid:cccc-3333";
        String id4 = "urn:uuid:dddd-4444";
        String id5 = "urn:uuid:eeee-5555";

        List<NormalizedEventDTO> events = List.of(
                createLoginEvent(id1, "user1", "10.0.0.1", baseTime.plusMinutes(0), false),
                createLoginEvent(id2, "user1", "10.0.0.1", baseTime.plusMinutes(1), false),
                createLoginEvent(id3, "user1", "10.0.0.1", baseTime.plusMinutes(2), false),
                createLoginEvent(id4, "user1", "10.0.0.1", baseTime.plusMinutes(3), false),
                createLoginEvent(id5, "user1", "10.0.0.1", baseTime.plusMinutes(4), false)
        );

        Set<String> originalIds = events.stream().map(NormalizedEventDTO::getEventId).collect(Collectors.toSet());

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        assertThat(originalIds).contains(findings.get(0).getEventId());
        assertThat(findings.get(0).getEventId()).isEqualTo(id5);
    }

    @Test
    @DisplayName("10. Empty input or null input returns no findings")
    void testEmptyAndNullInputReturnsNoFindings() {
        assertThat(service.detect(Collections.emptyList())).isEmpty();
        assertThat(service.detect(null)).isEmpty();
    }

    @Test
    @DisplayName("11. Unsorted input events are safely sorted by timestamp and detected")
    void testUnsortedEventsAreSafelySortedAndDetected() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);
        // Events in shuffled order: minute 4, 1, 0, 3, 2
        List<NormalizedEventDTO> events = List.of(
                createLoginEvent("ev-5", "user1", "10.0.0.1", baseTime.plusMinutes(4), false),
                createLoginEvent("ev-2", "user1", "10.0.0.1", baseTime.plusMinutes(1), false),
                createLoginEvent("ev-1", "user1", "10.0.0.1", baseTime.plusMinutes(0), false),
                createLoginEvent("ev-4", "user1", "10.0.0.1", baseTime.plusMinutes(3), false),
                createLoginEvent("ev-3", "user1", "10.0.0.1", baseTime.plusMinutes(2), false)
        );

        List<AnomalyFindingDTO> findings = service.detect(events);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getEventId()).isEqualTo("ev-5");
    }

    @Test
    @DisplayName("12. Mixed event types ignore non-LOGIN events")
    void testIgnoresNonLoginEvents() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);

        NormalizedEventDTO networkEvent = NormalizedEventDTO.builder()
                .eventId("net-1")
                .caseId("c-1")
                .timestamp(baseTime)
                .eventType("NETWORK_EVENT")
                .ipAddress("198.51.100.42")
                .bytes(5000000L)
                .sourceEvidenceId("ev-net")
                .build();

        NormalizedEventDTO fileEvent = NormalizedEventDTO.builder()
                .eventId("file-1")
                .caseId("c-1")
                .timestamp(baseTime.plusMinutes(1))
                .eventType("FILE_ACCESS")
                .user("attacker")
                .fileName("secret.doc")
                .action("READ")
                .sourceEvidenceId("ev-file")
                .build();

        List<NormalizedEventDTO> events = List.of(networkEvent, fileEvent);

        assertThat(service.detect(events)).isEmpty();
    }

    @Test
    @DisplayName("13. Severity scales with attempt count: HIGH at 2x threshold, CRITICAL at 3x threshold")
    void testSeverityScaling() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 26, 10, 0, 0);

        // 10 attempts (2x threshold 5) -> HIGH
        List<NormalizedEventDTO> highEvents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            highEvents.add(createLoginEvent("ev-" + i, "user1", "1.1.1.1", baseTime.plusSeconds(i * 10), false));
        }
        List<AnomalyFindingDTO> highFindings = service.detect(highEvents);
        assertThat(highFindings).hasSize(1);
        assertThat(highFindings.get(0).getSeverity()).isEqualTo(AnomalySeverity.HIGH);
        assertThat(highFindings.get(0).getScore()).isEqualTo(0.85);

        // 15 attempts (3x threshold 5) -> CRITICAL
        List<NormalizedEventDTO> critEvents = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            critEvents.add(createLoginEvent("crit-" + i, "user2", "2.2.2.2", baseTime.plusSeconds(i * 10), false));
        }
        List<AnomalyFindingDTO> critFindings = service.detect(critEvents);
        assertThat(critFindings).hasSize(1);
        assertThat(critFindings.get(0).getSeverity()).isEqualTo(AnomalySeverity.CRITICAL);
        assertThat(critFindings.get(0).getScore()).isGreaterThanOrEqualTo(0.95);
    }
}
