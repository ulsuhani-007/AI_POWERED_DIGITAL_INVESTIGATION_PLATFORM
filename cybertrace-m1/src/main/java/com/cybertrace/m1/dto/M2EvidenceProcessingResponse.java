package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class M2EvidenceProcessingResponse {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("source_evidence_id")
    private String sourceEvidenceId;

    @JsonProperty("total_processed_rows")
    private long totalProcessedRows;

    @JsonProperty("valid_events_count")
    private long validEventsCount;

    @JsonProperty("error_count")
    private long errorCount;

    @JsonProperty("events")
    private List<NormalizedEventDTO> events = new ArrayList<>();

    @JsonProperty("errors")
    private List<Map<String, Object>> errors = new ArrayList<>();

    public M2EvidenceProcessingResponse() {}

    public M2EvidenceProcessingResponse(String caseId, String sourceEvidenceId, long totalProcessedRows,
                                      long validEventsCount, long errorCount,
                                      List<NormalizedEventDTO> events, List<Map<String, Object>> errors) {
        this.caseId = caseId;
        this.sourceEvidenceId = sourceEvidenceId;
        this.totalProcessedRows = totalProcessedRows;
        this.validEventsCount = validEventsCount;
        this.errorCount = errorCount;
        this.events = events != null ? events : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getSourceEvidenceId() {
        return sourceEvidenceId;
    }

    public void setSourceEvidenceId(String sourceEvidenceId) {
        this.sourceEvidenceId = sourceEvidenceId;
    }

    public long getTotalProcessedRows() {
        return totalProcessedRows;
    }

    public void setTotalProcessedRows(long totalProcessedRows) {
        this.totalProcessedRows = totalProcessedRows;
    }

    public long getValidEventsCount() {
        return validEventsCount;
    }

    public void setValidEventsCount(long validEventsCount) {
        this.validEventsCount = validEventsCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public List<NormalizedEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<NormalizedEventDTO> events) {
        this.events = events != null ? events : new ArrayList<>();
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public void setErrors(List<Map<String, Object>> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}

