package com.cybertrace.m2.service;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.TimelineMergeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineServiceTest {

    private TimelineService timelineService;

    @BeforeEach
    void setUp() {
        timelineService = new TimelineService();
    }

    @Test
    @DisplayName("Should consolidate and sort events across multiple sources and cases")
    void testMergeTimeline() {
        NormalizedEventDTO e1 = NormalizedEventDTO.builder()
                .eventId("urn:uuid:00000000-0000-0000-0000-000000000001")
                .caseId("case-A")
                .sourceEvidenceId("ev-logins")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 0, 0))
                .eventType(EventType.LOGIN)
                .user("user1")
                .build();

        NormalizedEventDTO e2 = NormalizedEventDTO.builder()
                .eventId("urn:uuid:00000000-0000-0000-0000-000000000002")
                .caseId("case-A")
                .sourceEvidenceId("ev-network")
                .timestamp(LocalDateTime.of(2026, 10, 26, 9, 30, 0))
                .eventType(EventType.NETWORK_EVENT)
                .ipAddress("10.0.0.1")
                .build();

        NormalizedEventDTO e3 = NormalizedEventDTO.builder()
                .eventId("urn:uuid:00000000-0000-0000-0000-000000000003")
                .caseId("case-B")
                .sourceEvidenceId("ev-files")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .eventType(EventType.FILE_ACCESS)
                .fileName("secret.doc")
                .build();

        TimelineMergeResponse response = timelineService.mergeTimeline(List.of(e1, e2, e3));

        assertThat(response.getTotalEvents()).isEqualTo(3);
        assertThat(response.getStartTime()).isEqualTo(LocalDateTime.of(2026, 10, 26, 9, 30, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 0));
        assertThat(response.getCaseIds()).containsExactlyInAnyOrder("case-A", "case-B");
        assertThat(response.getSourceEvidenceIds()).containsExactlyInAnyOrder("ev-logins", "ev-network", "ev-files");

        List<NormalizedEventDTO> sorted = response.getEvents();
        assertThat(sorted.get(0).getEventId()).isEqualTo("urn:uuid:00000000-0000-0000-0000-000000000002");
        assertThat(sorted.get(1).getEventId()).isEqualTo("urn:uuid:00000000-0000-0000-0000-000000000001");
        assertThat(sorted.get(2).getEventId()).isEqualTo("urn:uuid:00000000-0000-0000-0000-000000000003");
    }

    @Test
    @DisplayName("Should handle empty or null incoming events cleanly")
    void testEmptyEvents() {
        TimelineMergeResponse response = timelineService.mergeTimeline(Collections.emptyList());
        assertThat(response.getTotalEvents()).isEqualTo(0);
        assertThat(response.getStartTime()).isNull();
        assertThat(response.getEndTime()).isNull();
        assertThat(response.getEvents()).isEmpty();
    }
}
