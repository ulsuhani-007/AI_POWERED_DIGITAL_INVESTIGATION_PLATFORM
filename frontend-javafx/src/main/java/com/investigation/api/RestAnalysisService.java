package com.investigation.api;

import com.investigation.dto.AnalysisDto;
import com.investigation.dto.CaseDto;
import com.investigation.dto.FindingDto;
import com.investigation.model.AnalysisResultModel;
import com.investigation.model.DashboardStats;
import com.investigation.model.FindingModel;
import com.investigation.model.enums.Severity;
import com.investigation.service.AnalysisService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REST implementation for AI analysis & findings:
 * - GET /api/cases/{id}/analysis
 */
public class RestAnalysisService implements AnalysisService {

    private final ApiClient apiClient;

    public RestAnalysisService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public AnalysisResultModel getAnalysisByCase(String caseId) throws Exception {
        try {
            AnalysisDto dto = apiClient.get("/api/cases/" + caseId + "/analysis", AnalysisDto.class);
            return mapToModel(dto, caseId);
        } catch (java.io.IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("HTTP 404")) {
                return new AnalysisResultModel(caseId, 0, 0, Collections.emptyList(), Collections.emptyMap(),
                        "No analysis report has been generated yet for this case. Upload evidence files to initiate automated processing.", Instant.now());
            }
            throw e;
        }
    }

    @Override
    public DashboardStats getDashboardStats() throws Exception {
        // Aggregate statistics from cases endpoint
        List<CaseDto> cases = apiClient.getList("/api/cases", CaseDto.class);
        int totalCases = cases.size();
        int totalEvidence = cases.stream().mapToInt(CaseDto::getEvidenceCount).sum();
        int totalEvents = cases.stream().mapToInt(CaseDto::getEventCount).sum();
        int totalAnomalies = cases.stream().mapToInt(CaseDto::getAnomalyCount).sum();
        int openCases = (int) cases.stream().filter(c -> "OPEN".equalsIgnoreCase(c.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(c.getStatus())).count();

        int criticalAnomalies = 0;
        for (CaseDto c : cases) {
            if (c.getAnomalyCount() > 0 && c.getId() != null) {
                try {
                    AnalysisResultModel arm = getAnalysisByCase(c.getId());
                    if (arm != null && arm.getFindings() != null) {
                        for (FindingModel fm : arm.getFindings()) {
                            if (fm.getSeverity() == Severity.CRITICAL || fm.getSeverity() == Severity.HIGH) {
                                criticalAnomalies++;
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Non-blocking if analysis is not yet ready for a case
                }
            }
        }

        List<DashboardStats.ActivityItem> activity = new ArrayList<>();
        for (CaseDto c : cases) {
            activity.add(new DashboardStats.ActivityItem(
                    "act-" + c.getId(),
                    "Case " + c.getCaseNumber() + ": " + c.getTitle(),
                    (c.getAnomalyCount() > 0 ? "⚠️ " + c.getAnomalyCount() + " anomalies detected. " : "Normal baseline. ") +
                            "Lead: " + (c.getLeadInvestigator() != null ? c.getLeadInvestigator() : "Unassigned"),
                    "CASE_STATUS",
                    c.getUpdatedAt() != null ? c.getUpdatedAt() : (c.getCreatedAt() != null ? c.getCreatedAt() : Instant.now()),
                    c.getCaseNumber()
            ));
        }

        return new DashboardStats(totalCases, totalEvidence, totalEvents, totalAnomalies, openCases, criticalAnomalies, activity);
    }

    private AnalysisResultModel mapToModel(AnalysisDto dto, String caseId) {
        if (dto == null) {
            return new AnalysisResultModel(caseId, 0, 0, Collections.emptyList(), Collections.emptyMap(), "No analysis available from backend.", Instant.now());
        }

        List<FindingModel> findings = new ArrayList<>();
        if (dto.getFindings() != null) {
            for (FindingDto f : dto.getFindings()) {
                String eventId = f.getEventId();
                String fId = f.getId();
                if (fId == null || fId.isBlank()) {
                    fId = eventId != null ? "fnd-" + eventId : java.util.UUID.randomUUID().toString();
                }
                findings.add(new FindingModel(
                        fId,
                        eventId,
                        f.getTimestamp() != null ? f.getTimestamp() : Instant.now(),
                        f.getEventType() != null ? f.getEventType() : "SUSPICIOUS_ACTIVITY",
                        f.getReason() != null ? f.getReason() : "Anomaly detected by AI service",
                        Severity.fromString(f.getSeverity()),
                        f.getScore(),
                        f.getDetectionMethod() != null ? f.getDetectionMethod() : "AI Anomaly Detection",
                        f.getAffectedEntity() != null ? f.getAffectedEntity() : "Forensic Event " + (eventId != null ? eventId : ""),
                        f.getSuggestedAction() != null ? f.getSuggestedAction() : "Review correlated events and isolate involved hosts."
                ));
            }
        }

        return new AnalysisResultModel(
                dto.getCaseId() != null ? dto.getCaseId() : caseId,
                dto.getTotalEvents(),
                dto.getTotalAnomalies(),
                findings,
                dto.getStatistics(),
                dto.getSummary(),
                dto.getGeneratedAt() != null ? dto.getGeneratedAt() : Instant.now()
        );
    }
}
