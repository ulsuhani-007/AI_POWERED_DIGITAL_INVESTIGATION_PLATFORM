package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisDTO {

    @JsonProperty("case_id")
    @JsonAlias({"caseId"})
    private String caseId;

    @JsonProperty("total_events")
    @JsonAlias({"totalEvents"})
    private Integer totalEvents = 0;

    @JsonProperty("total_anomalies")
    @JsonAlias({"totalAnomalies"})
    private Integer totalAnomalies = 0;

    @JsonProperty("findings")
    private List<FindingDTO> findings = new ArrayList<>();

    @JsonProperty("statistics")
    private Map<String, Object> statistics = new LinkedHashMap<>();

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("generated_at")
    @JsonAlias({"generatedAt"})
    private Instant generatedAt;

    public AnalysisDTO() {}

    public AnalysisDTO(String caseId, Integer totalEvents, Integer totalAnomalies,
                       List<FindingDTO> findings, Map<String, Object> statistics,
                       String summary, Instant generatedAt) {
        this.caseId = caseId;
        this.totalEvents = totalEvents != null ? totalEvents : 0;
        this.totalAnomalies = totalAnomalies != null ? totalAnomalies : 0;
        this.findings = findings != null ? findings : new ArrayList<>();
        this.statistics = statistics != null ? statistics : new LinkedHashMap<>();
        this.summary = summary;
        this.generatedAt = generatedAt != null ? generatedAt : Instant.now();
    }

    // JavaFX camelCase compatibility getter aliases
    @JsonProperty("caseId")
    public String getCaseIdCamel() {
        return caseId;
    }

    @JsonProperty("totalEvents")
    public Integer getTotalEventsCamel() {
        return totalEvents;
    }

    @JsonProperty("totalAnomalies")
    public Integer getTotalAnomaliesCamel() {
        return totalAnomalies;
    }

    @JsonProperty("generatedAt")
    public Instant getGeneratedAtCamel() {
        return generatedAt;
    }

    // Standard getters and setters
    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Integer getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(Integer totalEvents) {
        this.totalEvents = totalEvents;
    }

    public Integer getTotalAnomalies() {
        return totalAnomalies;
    }

    public void setTotalAnomalies(Integer totalAnomalies) {
        this.totalAnomalies = totalAnomalies;
    }

    public List<FindingDTO> getFindings() {
        return findings;
    }

    public void setFindings(List<FindingDTO> findings) {
        this.findings = findings != null ? findings : new ArrayList<>();
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics != null ? statistics : new LinkedHashMap<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}

