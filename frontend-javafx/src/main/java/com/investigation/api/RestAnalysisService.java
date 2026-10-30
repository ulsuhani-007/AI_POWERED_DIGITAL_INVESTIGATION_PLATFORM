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
        AnalysisDto dto = apiClient.get("/api/cases/" + caseId + "/analysis", AnalysisDto.class);
        return mapToModel(dto, caseId);
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

        List<DashboardStats.ActivityItem> activity = new ArrayList<>();
        for (CaseDto c : cases) {
            activity.add(new DashboardStats.ActivityItem(
                    "act-" + c.getId(),
                    "Case Active: " + c.getTitle(),
                    "Assigned to " + (c.getLeadInvestigator() != null ? c.getLeadInvestigator() : "Unassigned"),
                    "CASE_STATUS",
                    c.getUpdatedAt() != null ? c.getUpdatedAt() : Instant.now(),
                    c.getCaseNumber()
            ));
        }

        return new DashboardStats(totalCases, totalEvidence, totalEvents, totalAnomalies, openCases, 0, activity);
    }

    private AnalysisResultModel mapToModel(AnalysisDto dto, String caseId) {
        if (dto == null) {
            return new AnalysisResultModel(caseId, 0, 0, Collections.emptyList(), Collections.emptyMap(), "No analysis available from backend.", Instant.now());
        }

        List<FindingModel> findings = new ArrayList<>();
        if (dto.getFindings() != null) {
            for (FindingDto f : dto.getFindings()) {
                findings.add(new FindingModel(
                        f.getId() != null ? f.getId() : "fnd-" + f.getEventId(),
                        f.getEventId(),
                        f.getTimestamp(),
                        f.getEventType() != null ? f.getEventType() : "SUSPICIOUS_ACTIVITY",
                        f.getReason() != null ? f.getReason() : "Anomaly detected by AI service",
                        Severity.fromString(f.getSeverity()),
                        f.getScore(),
                        f.getDetectionMethod() != null ? f.getDetectionMethod() : "AI Anomaly Detection",
                        f.getAffectedEntity(),
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
