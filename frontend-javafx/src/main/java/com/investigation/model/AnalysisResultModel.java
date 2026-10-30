package com.investigation.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing the complete analysis output for a case.
 * Matches M3 AnalysisResult model.
 */
public class AnalysisResultModel {
    private String caseId;
    private int totalEvents;
    private int totalAnomalies;
    private List<FindingModel> findings = new ArrayList<>();
    private Map<String, Object> statistics = new HashMap<>();
    private String summary = "";
    private Instant generatedAt;

    public AnalysisResultModel() {
        this.generatedAt = Instant.now();
    }

    public AnalysisResultModel(String caseId, int totalEvents, int totalAnomalies,
                               List<FindingModel> findings, Map<String, Object> statistics,
                               String summary, Instant generatedAt) {
        this.caseId = caseId;
        this.totalEvents = totalEvents;
        this.totalAnomalies = totalAnomalies;
        this.findings = findings != null ? findings : new ArrayList<>();
        this.statistics = statistics != null ? statistics : new HashMap<>();
        this.summary = summary != null ? summary : "";
        this.generatedAt = generatedAt != null ? generatedAt : Instant.now();
    }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }

    public int getTotalAnomalies() { return totalAnomalies; }
    public void setTotalAnomalies(int totalAnomalies) { this.totalAnomalies = totalAnomalies; }

    public List<FindingModel> getFindings() { return findings; }
    public void setFindings(List<FindingModel> findings) { this.findings = findings; }

    public Map<String, Object> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
