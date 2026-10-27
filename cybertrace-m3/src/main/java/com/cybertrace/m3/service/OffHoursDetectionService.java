package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to detect suspicious digital forensic activity occurring outside configured working hours.
 * Analyzes sensitive operations (LOGIN and FILE_ACCESS events) and flags those taking place
 * during off-hours with graduated severity and scores based on temporal deviation.
 */
@Service
public class OffHoursDetectionService {

    public static final int DEFAULT_START_HOUR = 9;
    public static final int DEFAULT_END_HOUR = 18;
    public static final String DETECTION_METHOD = "RULE_BASED_OFF_HOURS";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final int startHour;
    private final int endHour;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public OffHoursDetectionService(
            @Value("${cybertrace.m3.off-hours.start-hour:9}") int startHour,
            @Value("${cybertrace.m3.off-hours.end-hour:18}") int endHour
    ) {
        int validStart = (startHour >= 0 && startHour <= 23) ? startHour : DEFAULT_START_HOUR;
        int validEnd = (endHour >= 0 && endHour <= 23) ? endHour : DEFAULT_END_HOUR;

        this.startHour = validStart;
        this.endHour = validEnd;
        this.startTime = LocalTime.of(validStart, 0);
        this.endTime = LocalTime.of(validEnd, 0);
    }

    public OffHoursDetectionService() {
        this(DEFAULT_START_HOUR, DEFAULT_END_HOUR);
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Checks if a given time falls within the configured working hours.
     * The interval is inclusive of the exact boundaries:
     * - For daytime intervals (e.g. 09:00 to 18:00): [09:00:00, 18:00:00] is working hours.
     * - For cross-midnight intervals (e.g. 22:00 to 06:00): [22:00:00 through 06:00:00] is working hours.
     *
     * @param time time of day to evaluate
     * @return true if within working hours, false if off-hours
     */
    public boolean isWorkingHours(LocalTime time) {
        if (time == null) {
            return true;
        }

        if (startHour <= endHour) {
            // Normal daytime working interval (e.g. 09:00 - 18:00)
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        } else {
            // Cross-midnight working interval (e.g. 22:00 - 06:00)
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        }
    }

    /**
     * Calculates the minimum circular distance in minutes from working hours.
     *
     * @param time time of day
     * @return 0 if within working hours, otherwise distance in minutes to nearest boundary
     */
    public long minutesFromWorkingHours(LocalTime time) {
        if (isWorkingHours(time)) {
            return 0;
        }

        long diffBefore = Duration.between(time, startTime).toMinutes();
        if (diffBefore < 0) {
            diffBefore += 24 * 60;
        }

        long diffAfter = Duration.between(endTime, time).toMinutes();
        if (diffAfter < 0) {
            diffAfter += 24 * 60;
        }

        return Math.min(diffBefore, diffAfter);
    }

    /**
     * Analyzes normalized events and flags off-hours activity for LOGIN and FILE_ACCESS operations.
     *
     * @param events list of normalized events from M2
     * @return list of detected off-hours anomaly findings
     */
    public List<AnomalyFindingDTO> detect(List<NormalizedEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<AnomalyFindingDTO> findings = new ArrayList<>();

        for (NormalizedEventDTO event : events) {
            if (event == null) {
                continue;
            }

            // 1. Analyze ONLY LOGIN and FILE_ACCESS events; safely ignore other types or null
            if (!isTargetEventType(event.getEventType())) {
                continue;
            }

            // 2. Ignore events with missing timestamp
            if (event.getTimestamp() == null) {
                continue;
            }

            LocalTime eventTime = event.getTimestamp().toLocalTime();

            // 3. Evaluate working hour boundary
            if (!isWorkingHours(eventTime)) {
                long minutesAway = minutesFromWorkingHours(eventTime);

                AnomalySeverity severity = calculateSeverity(minutesAway);
                double score = calculateScore(minutesAway);
                String reason = buildReason(event, eventTime);

                AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                        .eventId(event.getEventId())
                        .timestamp(event.getTimestamp())
                        .eventType(event.getEventType())
                        .anomalyType(AnomalyType.OFF_HOURS)
                        .reason(reason)
                        .severity(severity)
                        .score(score)
                        .detectionMethod(DETECTION_METHOD)
                        .build();

                findings.add(finding);
            }
        }

        return findings;
    }

    private boolean isTargetEventType(String eventType) {
        if (eventType == null) {
            return false;
        }
        return "LOGIN".equalsIgnoreCase(eventType) || "FILE_ACCESS".equalsIgnoreCase(eventType);
    }

    private AnomalySeverity calculateSeverity(long minutesAway) {
        // Avoid marking every off-hours event CRITICAL automatically:
        // <= 2 hours outside window: LOW (e.g. staying slightly late or arriving early)
        // 2 to 5 hours outside window: MEDIUM (e.g. late evening / early morning)
        // > 5 hours outside window: HIGH (e.g. deep night / graveyard hours like 01:00-05:00)
        if (minutesAway > 300) {
            return AnomalySeverity.HIGH;
        } else if (minutesAway > 120) {
            return AnomalySeverity.MEDIUM;
        } else {
            return AnomalySeverity.LOW;
        }
    }

    private double calculateScore(long minutesAway) {
        // Scales proportionally from 0.35 up to 0.85 based on temporal distance (capped at 0.85)
        double score = 0.35 + Math.min(0.50, (minutesAway / 420.0) * 0.50);
        return Math.round(score * 100.0) / 100.0;
    }

    private String buildReason(NormalizedEventDTO event, LocalTime eventTime) {
        String timeStr = eventTime.format(TIME_FORMATTER);
        return String.format(
                "%s activity occurred at %s, outside configured working hours of %02d:00-%02d:00.",
                event.getEventType(), timeStr, startHour, endHour
        );
    }
}
