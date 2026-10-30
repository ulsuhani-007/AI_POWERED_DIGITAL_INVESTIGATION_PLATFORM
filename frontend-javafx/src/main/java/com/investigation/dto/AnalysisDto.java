package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for:
 * - GET /api/cases/{id}/analysis
 *
 * Implements M3 AnalysisResult model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisDto {

    @JsonProperty("caseId")
    @JsonAlias({"case_id"})
    private String caseId;

    @JsonProperty("totalEvents")
    @JsonAlias({"total_events"})
    private Integer totalEvents;

    @JsonProperty("totalAnomalies")
    @JsonAlias({"total_anomalies"})
    private Integer totalAnomalies;

    @JsonProperty("findings")
    private List<FindingDto> findings = new ArrayList<>();

    @JsonProperty("statistics")
    private Map<String, Object> statistics = new HashMap<>();

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("generatedAt")
    @JsonAlias({"generated_at"})
    private Instant generatedAt;

    public AnalysisDto() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public Integer getTotalEvents() { return totalEvents != null ? totalEvents : 0; }
    public void setTotalEvents(Integer totalEvents) { this.totalEvents = totalEvents; }

    public Integer getTotalAnomalies() { return totalAnomalies != null ? totalAnomalies : 0; }
    public void setTotalAnomalies(Integer totalAnomalies) { this.totalAnomalies = totalAnomalies; }

    public List<FindingDto> getFindings() { return findings != null ? findings : new ArrayList<>(); }
    public void setFindings(List<FindingDto> findings) { this.findings = findings; }

    public Map<String, Object> getStatistics() { return statistics != null ? statistics : new HashMap<>(); }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }

    public String getSummary() { return summary != null ? summary : ""; }
    public void setSummary(String summary) { this.summary = summary; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
