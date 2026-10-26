package com.cybertrace.m2.service;

import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.TimelineMergeResponse;
import com.cybertrace.m2.util.NormalizedEventComparator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service for merging normalized events across multiple evidence sources
 * into a consolidated chronological forensic timeline.
 */
@Service
public class TimelineService {

    /**
     * Consolidates, deduplicates, and sorts events into a unified chronological timeline.
     *
     * @param incomingEvents raw list of normalized events from various sources
     * @return TimelineMergeResponse containing sorted timeline and summary metadata
     */
    public TimelineMergeResponse mergeTimeline(List<NormalizedEventDTO> incomingEvents) {
        if (incomingEvents == null || incomingEvents.isEmpty()) {
            return TimelineMergeResponse.builder()
                    .totalEvents(0)
                    .startTime(null)
                    .endTime(null)
                    .caseIds(Collections.emptyList())
                    .sourceEvidenceIds(Collections.emptyList())
                    .events(Collections.emptyList())
                    .build();
        }

        // Filter non-null and copy
        List<NormalizedEventDTO> validEvents = incomingEvents.stream()
                .filter(Objects::nonNull)
                .sorted(NormalizedEventComparator.INSTANCE)
                .toList();

        if (validEvents.isEmpty()) {
            return TimelineMergeResponse.builder()
                    .totalEvents(0)
                    .startTime(null)
                    .endTime(null)
                    .caseIds(Collections.emptyList())
                    .sourceEvidenceIds(Collections.emptyList())
                    .events(Collections.emptyList())
                    .build();
        }

        LocalDateTime startTime = validEvents.getFirst().getTimestamp();
        LocalDateTime endTime = validEvents.getLast().getTimestamp();

        Set<String> uniqueCaseIds = new HashSet<>();
        Set<String> uniqueEvidenceIds = new HashSet<>();

        for (NormalizedEventDTO event : validEvents) {
            if (event.getCaseId() != null) {
                uniqueCaseIds.add(event.getCaseId());
            }
            if (event.getSourceEvidenceId() != null) {
                uniqueEvidenceIds.add(event.getSourceEvidenceId());
            }
        }

        return TimelineMergeResponse.builder()
                .totalEvents(validEvents.size())
                .startTime(startTime)
                .endTime(endTime)
                .caseIds(new ArrayList<>(uniqueCaseIds))
                .sourceEvidenceIds(new ArrayList<>(uniqueEvidenceIds))
                .events(validEvents)
                .build();
    }
}
