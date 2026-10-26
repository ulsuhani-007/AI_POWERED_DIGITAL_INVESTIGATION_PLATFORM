package com.cybertrace.m2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the complete result of an evidence file upload and processing job.
 */
public class EvidenceProcessingResponse {

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
    private List<ProcessingErrorDTO> errors = new ArrayList<>();

    public EvidenceProcessingResponse() {
    }

    public EvidenceProcessingResponse(
            String caseId,
            String sourceEvidenceId,
            long totalProcessedRows,
            long validEventsCount,
            long errorCount,
            List<NormalizedEventDTO> events,
            List<ProcessingErrorDTO> errors
    ) {
        this.caseId = caseId;
        this.sourceEvidenceId = sourceEvidenceId;
        this.totalProcessedRows = totalProcessedRows;
        this.validEventsCount = validEventsCount;
        this.errorCount = errorCount;
        this.events = events != null ? events : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
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
        this.events = events;
    }

    public List<ProcessingErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<ProcessingErrorDTO> errors) {
        this.errors = errors;
    }

    public static class Builder {
        private String caseId;
        private String sourceEvidenceId;
        private long totalProcessedRows;
        private long validEventsCount;
        private long errorCount;
        private List<NormalizedEventDTO> events = new ArrayList<>();
        private List<ProcessingErrorDTO> errors = new ArrayList<>();

        public Builder caseId(String caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder sourceEvidenceId(String sourceEvidenceId) {
            this.sourceEvidenceId = sourceEvidenceId;
            return this;
        }

        public Builder totalProcessedRows(long totalProcessedRows) {
            this.totalProcessedRows = totalProcessedRows;
            return this;
        }

        public Builder validEventsCount(long validEventsCount) {
            this.validEventsCount = validEventsCount;
            return this;
        }

        public Builder errorCount(long errorCount) {
            this.errorCount = errorCount;
            return this;
        }

        public Builder events(List<NormalizedEventDTO> events) {
            this.events = events != null ? events : new ArrayList<>();
            return this;
        }

        public Builder errors(List<ProcessingErrorDTO> errors) {
            this.errors = errors != null ? errors : new ArrayList<>();
            return this;
        }

        public EvidenceProcessingResponse build() {
            return new EvidenceProcessingResponse(caseId, sourceEvidenceId, totalProcessedRows,
                    validEventsCount, errorCount, events, errors);
        }
    }
}
