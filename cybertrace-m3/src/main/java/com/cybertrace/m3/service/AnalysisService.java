package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnalysisReportDTO;
import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.NormalizedEventDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central analysis orchestration service for CyberTrace AI Module 3 (M3).
 * Executes all forensic anomaly detectors across normalized digital investigation events,
 * consolidates findings into a deduplicated, chronologically sorted timeline, and
 * compiles investigation baseline statistics.
 */
@Service
public class AnalysisService {

    private final BruteForceDetectionService bruteForceService;
    private final NetworkVolumeDetectionService networkVolumeService;
    private final OffHoursDetectionService offHoursService;
    private final MachineLearningAnomalyDetectionService machineLearningService;
    private final InvestigationSummaryService summaryService;

    public AnalysisService(
            BruteForceDetectionService bruteForceService,
            NetworkVolumeDetectionService networkVolumeService,
            OffHoursDetectionService offHoursService,
            MachineLearningAnomalyDetectionService machineLearningService,
            InvestigationSummaryService summaryService
    ) {
        this.bruteForceService = Objects.requireNonNull(bruteForceService, "bruteForceService must not be null");
        this.networkVolumeService = Objects.requireNonNull(networkVolumeService, "networkVolumeService must not be null");
        this.offHoursService = Objects.requireNonNull(offHoursService, "offHoursService must not be null");
        this.machineLearningService = Objects.requireNonNull(machineLearningService, "machineLearningService must not be null");
        this.summaryService = Objects.requireNonNull(summaryService, "summaryService must not be null");
    }

    public AnalysisService(
            BruteForceDetectionService bruteForceService,
            NetworkVolumeDetectionService networkVolumeService,
            OffHoursDetectionService offHoursService,
            MachineLearningAnomalyDetectionService machineLearningService
    ) {
        this(bruteForceService, networkVolumeService, offHoursService, machineLearningService, new InvestigationSummaryService());
    }

    public AnalysisService(
            BruteForceDetectionService bruteForceService,
            NetworkVolumeDetectionService networkVolumeService,
            OffHoursDetectionService offHoursService
    ) {
        this(bruteForceService, networkVolumeService, offHoursService, new MachineLearningAnomalyDetectionService(), new InvestigationSummaryService());
    }

    public AnalysisService() {
        this(
                new BruteForceDetectionService(),
                new NetworkVolumeDetectionService(),
                new OffHoursDetectionService(),
                new MachineLearningAnomalyDetectionService(),
                new InvestigationSummaryService()
        );
    }

    /**
     * Executes forensic analysis across the provided event batch.
     *
     * @param events list of normalized evidence events from M2
     * @return comprehensive AnalysisReportDTO containing findings and statistics
     */
    public AnalysisReportDTO analyze(List<NormalizedEventDTO> events) {
        // Safe handling for null or empty input
        if (events == null || events.isEmpty()) {
            return buildEmptyReport();
        }

        // 1. Run all four forensic anomaly detectors without mutating input
        List<AnomalyFindingDTO> combinedFindings = new ArrayList<>();
        combinedFindings.addAll(bruteForceService.detect(events));
        combinedFindings.addAll(networkVolumeService.detect(events));
        combinedFindings.addAll(offHoursService.detect(events));
        combinedFindings.addAll(machineLearningService.detect(events));

        // 2. Deduplicate exact duplicate findings
        Set<AnomalyFindingDTO> deduplicatedFindings = new LinkedHashSet<>(combinedFindings);

        // 3. Sort findings chronologically by timestamp, with deterministic secondary ordering
        List<AnomalyFindingDTO> sortedFindings = deduplicatedFindings.stream()
                .sorted(Comparator.comparing(AnomalyFindingDTO::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AnomalyFindingDTO::getEventId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(f -> f.getAnomalyType() != null ? f.getAnomalyType().name() : "", Comparator.naturalOrder()))
                .collect(Collectors.toList());

        // 4. Calculate baseline statistics
        int totalEvents = events.size();
        int totalAnomalies = sortedFindings.size();
        Map<String, Object> statistics = calculateStatistics(events, sortedFindings);

        // 5. Build report and generate forensic investigation summary
        AnalysisReportDTO report = AnalysisReportDTO.builder()
                .totalEvents(totalEvents)
                .totalAnomalies(totalAnomalies)
                .findings(sortedFindings)
                .statistics(statistics)
                .summary("")
                .build();

        String summary = summaryService.generateSummary(report);
        report.setSummary(summary);

        return report;
    }

    private Map<String, Object> calculateStatistics(List<NormalizedEventDTO> events, List<AnomalyFindingDTO> findings) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_events", events.size());

        // Events by event_type
        Map<String, Integer> eventsByType = new LinkedHashMap<>();
        // Events by source_evidence_id
        Map<String, Integer> eventsBySource = new LinkedHashMap<>();

        for (NormalizedEventDTO event : events) {
            if (event == null) continue;

            String type = event.getEventType() != null ? event.getEventType() : "<UNKNOWN_TYPE>";
            eventsByType.put(type, eventsByType.getOrDefault(type, 0) + 1);

            String source = event.getSourceEvidenceId() != null ? event.getSourceEvidenceId() : "<UNKNOWN_SOURCE>";
            eventsBySource.put(source, eventsBySource.getOrDefault(source, 0) + 1);
        }

        stats.put("events_by_type", eventsByType);
        stats.put("events_by_source", eventsBySource);

        // Findings by anomaly_type
        Map<String, Integer> findingsByAnomalyType = new LinkedHashMap<>();
        // Findings by severity
        Map<String, Integer> findingsBySeverity = new LinkedHashMap<>();

        for (AnomalyFindingDTO finding : findings) {
            String anomalyType = finding.getAnomalyType() != null ? finding.getAnomalyType().name() : "<UNKNOWN_ANOMALY>";
            findingsByAnomalyType.put(anomalyType, findingsByAnomalyType.getOrDefault(anomalyType, 0) + 1);

            String severity = finding.getSeverity() != null ? finding.getSeverity().name() : "<UNKNOWN_SEVERITY>";
            findingsBySeverity.put(severity, findingsBySeverity.getOrDefault(severity, 0) + 1);
        }

        stats.put("findings_by_anomaly_type", findingsByAnomalyType);
        stats.put("findings_by_severity", findingsBySeverity);

        return stats;
    }

    private AnalysisReportDTO buildEmptyReport() {
        Map<String, Object> emptyStats = new LinkedHashMap<>();
        emptyStats.put("total_events", 0);
        emptyStats.put("events_by_type", Collections.emptyMap());
        emptyStats.put("events_by_source", Collections.emptyMap());
        emptyStats.put("findings_by_anomaly_type", Collections.emptyMap());
        emptyStats.put("findings_by_severity", Collections.emptyMap());

        AnalysisReportDTO report = AnalysisReportDTO.builder()
                .totalEvents(0)
                .totalAnomalies(0)
                .findings(Collections.emptyList())
                .statistics(emptyStats)
                .summary("")
                .build();

        String summary = summaryService.generateSummary(report);
        report.setSummary(summary);
        return report;
    }

    public BruteForceDetectionService getBruteForceService() {
        return bruteForceService;
    }

    public NetworkVolumeDetectionService getNetworkVolumeService() {
        return networkVolumeService;
    }

    public OffHoursDetectionService getOffHoursService() {
        return offHoursService;
    }

    public MachineLearningAnomalyDetectionService getMachineLearningService() {
        return machineLearningService;
    }

    public InvestigationSummaryService getSummaryService() {
        return summaryService;
    }
}
