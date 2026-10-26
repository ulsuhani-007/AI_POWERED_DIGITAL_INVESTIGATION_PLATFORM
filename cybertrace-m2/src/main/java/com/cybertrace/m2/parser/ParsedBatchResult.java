package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.ProcessingErrorDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Result bundle produced by an EvidenceParser containing successfully normalized events
 * and captured row errors.
 */
public class ParsedBatchResult {

    private List<NormalizedEventDTO> events = new ArrayList<>();
    private List<ProcessingErrorDTO> errors = new ArrayList<>();
    private long totalProcessedRows;

    public ParsedBatchResult() {
    }

    public ParsedBatchResult(List<NormalizedEventDTO> events, List<ProcessingErrorDTO> errors, long totalProcessedRows) {
        this.events = events != null ? events : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
        this.totalProcessedRows = totalProcessedRows;
    }

    public static Builder builder() {
        return new Builder();
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

    public long getTotalProcessedRows() {
        return totalProcessedRows;
    }

    public void setTotalProcessedRows(long totalProcessedRows) {
        this.totalProcessedRows = totalProcessedRows;
    }

    public static class Builder {
        private List<NormalizedEventDTO> events = new ArrayList<>();
        private List<ProcessingErrorDTO> errors = new ArrayList<>();
        private long totalProcessedRows;

        public Builder events(List<NormalizedEventDTO> events) {
            this.events = events != null ? events : new ArrayList<>();
            return this;
        }

        public Builder errors(List<ProcessingErrorDTO> errors) {
            this.errors = errors != null ? errors : new ArrayList<>();
            return this;
        }

        public Builder totalProcessedRows(long totalProcessedRows) {
            this.totalProcessedRows = totalProcessedRows;
            return this;
        }

        public ParsedBatchResult build() {
            return new ParsedBatchResult(events, errors, totalProcessedRows);
        }
    }
}
