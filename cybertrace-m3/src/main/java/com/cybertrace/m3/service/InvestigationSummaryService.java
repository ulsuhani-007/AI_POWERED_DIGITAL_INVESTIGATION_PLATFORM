package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnalysisReportDTO;
import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates concise, professional, and deterministic forensic investigation summaries
 * derived strictly from the analysis findings and metrics in {@link AnalysisReportDTO}.
 */
@Service
public class InvestigationSummaryService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final List<AnomalySeverity> SEVERITY_PRECEDENCE = List.of(
            AnomalySeverity.CRITICAL,
            AnomalySeverity.HIGH,
            AnomalySeverity.MEDIUM,
            AnomalySeverity.LOW
    );

    /**
     * Generates a deterministic plain-text forensic summary from an AnalysisReportDTO.
     *
     * @param report the analysis report containing findings and baseline metrics
     * @return plain-text formatted investigation summary
     */
    public String generateSummary(AnalysisReportDTO report) {
        if (report == null) {
            return "Overall Analysis:\n" +
                    "0 events analyzed across the timeline; 0 anomalies identified.\n\n" +
                    "Investigation Conclusion:\n" +
                    "No anomalies detected. No report data available for analysis.";
        }

        int totalEvents = report.getTotalEvents();
        List<AnomalyFindingDTO> findings = report.getFindings() != null ? report.getFindings() : Collections.emptyList();
        int totalAnomalies = findings.size();

        StringBuilder sb = new StringBuilder();

        // 1. Overall analysis
        sb.append("Overall Analysis:\n");
        sb.append(String.format("%d events analyzed across the timeline; %d %s identified.",
                totalEvents,
                totalAnomalies,
                totalAnomalies == 1 ? "anomaly" : "anomalies"));

        // If no anomalies exist, provide immediate conclusion and return
        if (findings.isEmpty()) {
            sb.append("\n\nInvestigation Conclusion:\n");
            sb.append("No anomalies detected. Analyzed events conform to expected baseline patterns.");
            return sb.toString();
        }

        // 2. Finding breakdown (type counts and severity distribution)
        Map<String, Integer> byType = computeFindingsByType(report, findings);
        Map<String, Integer> bySeverity = computeFindingsBySeverity(report, findings);

        sb.append("\n\nFinding Breakdown:\n");

        // Format Anomaly Types alphabetically for deterministic ordering
        String typeBreakdown = byType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> String.format("%s (%d)", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
        sb.append("- Anomaly Types: ").append(typeBreakdown).append("\n");

        // Format Severity Distribution by precedence (CRITICAL -> HIGH -> MEDIUM -> LOW)
        List<String> severityEntries = new ArrayList<>();
        for (AnomalySeverity sev : SEVERITY_PRECEDENCE) {
            if (bySeverity.containsKey(sev.name())) {
                severityEntries.add(String.format("%s (%d)", sev.name(), bySeverity.get(sev.name())));
            }
        }
        for (Map.Entry<String, Integer> entry : bySeverity.entrySet()) {
            if (!isStandardSeverity(entry.getKey())) {
                severityEntries.add(String.format("%s (%d)", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("- Severity Distribution: ").append(String.join(", ", severityEntries));

        // 3. Important findings (highest severity present)
        AnomalySeverity highestSeverity = determineHighestSeverity(findings);
        List<AnomalyFindingDTO> highestFindings = findings.stream()
                .filter(f -> f.getSeverity() == highestSeverity)
                .sorted(Comparator.comparing(AnomalyFindingDTO::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AnomalyFindingDTO::getEventId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(f -> f.getAnomalyType() != null ? f.getAnomalyType().name() : "", Comparator.naturalOrder()))
                .toList();

        sb.append("\n\nKey Findings:\n");
        for (AnomalyFindingDTO finding : highestFindings) {
            sb.append("- ").append(formatKeyFinding(finding)).append("\n");
        }
        // Remove trailing newline from findings block
        if (!highestFindings.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }

        // 4. Investigation conclusion
        sb.append("\n\nInvestigation Conclusion:\n");
        sb.append("Suspicious activity identified requiring analyst review. Prioritized findings warrant forensic inspection.");

        return sb.toString();
    }

    private String formatKeyFinding(AnomalyFindingDTO finding) {
        StringBuilder fb = new StringBuilder();

        // Severity tag
        String sevStr = finding.getSeverity() != null ? finding.getSeverity().name() : "UNKNOWN";
        fb.append("[").append(sevStr).append("] ");

        // Anomaly type
        String typeStr = finding.getAnomalyType() != null ? finding.getAnomalyType().name() : "ANOMALY";
        fb.append(typeStr);

        // Event ID
        if (finding.getEventId() != null && !finding.getEventId().isBlank()) {
            fb.append(" on event '").append(finding.getEventId()).append("'");
        }

        // Timestamp
        if (finding.getTimestamp() != null) {
            fb.append(" at ").append(finding.getTimestamp().format(TIMESTAMP_FORMATTER));
        }

        // Reason
        String reason = (finding.getReason() != null && !finding.getReason().isBlank())
                ? finding.getReason().trim()
                : "Anomalous forensic pattern identified.";
        fb.append(": ").append(reason);

        // Score (specifically identifying ML Score when MACHINE_LEARNING)
        if (finding.getScore() != null) {
            if (finding.getAnomalyType() == AnomalyType.MACHINE_LEARNING) {
                fb.append(String.format(Locale.US, " (ML Score: %.4f)", finding.getScore()));
            } else {
                fb.append(String.format(Locale.US, " (Score: %.4f)", finding.getScore()));
            }
        }

        return fb.toString();
    }

    private AnomalySeverity determineHighestSeverity(List<AnomalyFindingDTO> findings) {
        for (AnomalySeverity sev : SEVERITY_PRECEDENCE) {
            boolean present = findings.stream().anyMatch(f -> f.getSeverity() == sev);
            if (present) {
                return sev;
            }
        }
        return findings.isEmpty() ? AnomalySeverity.LOW : findings.get(0).getSeverity();
    }

    private boolean isStandardSeverity(String name) {
        return "CRITICAL".equalsIgnoreCase(name) ||
                "HIGH".equalsIgnoreCase(name) ||
                "MEDIUM".equalsIgnoreCase(name) ||
                "LOW".equalsIgnoreCase(name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> computeFindingsByType(AnalysisReportDTO report, List<AnomalyFindingDTO> findings) {
        if (report.getStatistics() != null && report.getStatistics().containsKey("findings_by_anomaly_type")) {
            Object obj = report.getStatistics().get("findings_by_anomaly_type");
            if (obj instanceof Map) {
                return (Map<String, Integer>) obj;
            }
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AnomalyFindingDTO f : findings) {
            String type = f.getAnomalyType() != null ? f.getAnomalyType().name() : "UNKNOWN";
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        return counts;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> computeFindingsBySeverity(AnalysisReportDTO report, List<AnomalyFindingDTO> findings) {
        if (report.getStatistics() != null && report.getStatistics().containsKey("findings_by_severity")) {
            Object obj = report.getStatistics().get("findings_by_severity");
            if (obj instanceof Map) {
                return (Map<String, Integer>) obj;
            }
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AnomalyFindingDTO f : findings) {
            String sev = f.getSeverity() != null ? f.getSeverity().name() : "UNKNOWN";
            counts.put(sev, counts.getOrDefault(sev, 0) + 1);
        }
        return counts;
    }
}
