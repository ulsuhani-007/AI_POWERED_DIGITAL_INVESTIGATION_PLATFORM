package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import com.cybertrace.m3.util.EventStatusExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to detect brute-force login attacks from normalized forensic events.
 * Identifies bursts of failed login attempts grouped by user and IP address
 * that meet or exceed a configured threshold within a specified time window.
 */
@Service
public class BruteForceDetectionService {

    public static final int DEFAULT_THRESHOLD = 5;
    public static final Duration DEFAULT_TIME_WINDOW = Duration.ofMinutes(5);
    public static final String DETECTION_METHOD = "RULE_BASED_TIME_WINDOW";

    private final int threshold;
    private final Duration timeWindow;

    public BruteForceDetectionService(
            @Value("${cybertrace.m3.brute-force.threshold:5}") int threshold,
            @Value("${cybertrace.m3.brute-force.time-window-minutes:5}") int timeWindowMinutes
    ) {
        this.threshold = threshold > 0 ? threshold : DEFAULT_THRESHOLD;
        this.timeWindow = Duration.ofMinutes(timeWindowMinutes > 0 ? timeWindowMinutes : 5);
    }

    public BruteForceDetectionService(int threshold, Duration timeWindow) {
        this.threshold = threshold > 0 ? threshold : DEFAULT_THRESHOLD;
        this.timeWindow = (timeWindow != null && !timeWindow.isNegative() && !timeWindow.isZero())
                ? timeWindow
                : DEFAULT_TIME_WINDOW;
    }

    public BruteForceDetectionService() {
        this(DEFAULT_THRESHOLD, DEFAULT_TIME_WINDOW);
    }

    public int getThreshold() {
        return threshold;
    }

    public Duration getTimeWindow() {
        return timeWindow;
    }

    /**
     * Analyzes normalized events and flags suspicious brute-force login activity.
     *
     * @param events list of normalized events from M2
     * @return list of detected brute-force findings
     */
    public List<AnomalyFindingDTO> detect(List<NormalizedEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Filter only LOGIN events with status FAILED and a valid timestamp
        List<NormalizedEventDTO> failedLogins = events.stream()
                .filter(Objects::nonNull)
                .filter(e -> "LOGIN".equalsIgnoreCase(e.getEventType()))
                .filter(e -> e.getTimestamp() != null)
                .filter(e -> EventStatusExtractor.extractStatus(e) == EventStatusExtractor.LoginStatus.FAILED)
                .collect(Collectors.toList());

        if (failedLogins.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Group by (user, ip_address)
        Map<UserIpKey, List<NormalizedEventDTO>> grouped = failedLogins.stream()
                .collect(Collectors.groupingBy(UserIpKey::of, LinkedHashMap::new, Collectors.toList()));

        List<AnomalyFindingDTO> findings = new ArrayList<>();

        // 3. For each group, sort by timestamp ascending and identify bursts within timeWindow
        for (Map.Entry<UserIpKey, List<NormalizedEventDTO>> entry : grouped.entrySet()) {
            UserIpKey key = entry.getKey();
            List<NormalizedEventDTO> groupEvents = entry.getValue();

            // Safe sorting by timestamp ascending
            groupEvents.sort(Comparator.comparing(NormalizedEventDTO::getTimestamp));

            findings.addAll(detectBurstsForGroup(key, groupEvents));
        }

        return findings;
    }

    private List<AnomalyFindingDTO> detectBurstsForGroup(UserIpKey key, List<NormalizedEventDTO> sortedEvents) {
        List<AnomalyFindingDTO> groupFindings = new ArrayList<>();
        int n = sortedEvents.size();
        int i = 0;

        while (i < n) {
            LocalDateTime windowStart = sortedEvents.get(i).getTimestamp();
            LocalDateTime windowEnd = windowStart.plus(timeWindow);

            // Expand window to capture all attempts within the time window
            int j = i;
            while (j < n && !sortedEvents.get(j).getTimestamp().isAfter(windowEnd)) {
                j++;
            }

            int count = j - i;
            if (count >= threshold) {
                // The triggering event is the last attempt in this suspicious window
                NormalizedEventDTO triggeringEvent = sortedEvents.get(j - 1);

                AnomalySeverity severity = calculateSeverity(count);
                double score = calculateScore(count);
                String reason = buildReason(key, count, timeWindow);

                AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                        .eventId(triggeringEvent.getEventId())
                        .timestamp(triggeringEvent.getTimestamp())
                        .eventType("LOGIN")
                        .anomalyType(AnomalyType.BRUTE_FORCE)
                        .reason(reason)
                        .severity(severity)
                        .score(score)
                        .detectionMethod(DETECTION_METHOD)
                        .build();

                groupFindings.add(finding);

                // Advance past this incident to avoid duplicate findings for the same burst
                i = j;
            } else {
                i++;
            }
        }

        return groupFindings;
    }

    private AnomalySeverity calculateSeverity(int failedCount) {
        if (failedCount >= threshold * 3) {
            return AnomalySeverity.CRITICAL;
        } else if (failedCount >= threshold * 2) {
            return AnomalySeverity.HIGH;
        } else {
            return AnomalySeverity.MEDIUM;
        }
    }

    private double calculateScore(int failedCount) {
        // Base score 0.70 at threshold, scaling smoothly up to 0.99
        double score = 0.70 + Math.min(0.29, (failedCount - threshold) * 0.03);
        return Math.round(score * 100.0) / 100.0;
    }

    private String buildReason(UserIpKey key, int count, Duration window) {
        long minutes = window.toMinutes();
        String timeStr = minutes > 0
                ? minutes + " minute" + (minutes > 1 ? "s" : "")
                : window.toSeconds() + " seconds";

        return String.format(
                "Detected %d failed login attempts for user '%s' from IP '%s' within %s (threshold: %d)",
                count, key.user(), key.ipAddress(), timeStr, threshold
        );
    }

    /**
     * Composite key for grouping by user and ipAddress safely.
     */
    public record UserIpKey(String user, String ipAddress) {
        public static UserIpKey of(NormalizedEventDTO event) {
            String u = (event.getUser() != null && !event.getUser().isBlank())
                    ? event.getUser().trim()
                    : "<UNKNOWN_USER>";
            String ip = (event.getIpAddress() != null && !event.getIpAddress().isBlank())
                    ? event.getIpAddress().trim()
                    : "<UNKNOWN_IP>";
            return new UserIpKey(u, ip);
        }
    }
}
