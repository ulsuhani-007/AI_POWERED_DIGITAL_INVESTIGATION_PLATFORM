package com.cybertrace.m2.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated chronological timeline response with metadata summaries.
 */
public class TimelineMergeResponse {

    @JsonProperty("total_events")
    private long totalEvents;

    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @JsonProperty("case_ids")
    private List<String> caseIds = new ArrayList<>();

    @JsonProperty("source_evidence_ids")
    private List<String> sourceEvidenceIds = new ArrayList<>();

    @JsonProperty("events")
    private List<NormalizedEventDTO> events = new ArrayList<>();

    public TimelineMergeResponse() {
    }

    public TimelineMergeResponse(
            long totalEvents,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<String> caseIds,
            List<String> sourceEvidenceIds,
            List<NormalizedEventDTO> events
    ) {
        this.totalEvents = totalEvents;
        this.startTime = startTime;
        this.endTime = endTime;
        this.caseIds = caseIds != null ? caseIds : new ArrayList<>();
        this.sourceEvidenceIds = sourceEvidenceIds != null ? sourceEvidenceIds : new ArrayList<>();
        this.events = events != null ? events : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getCaseIds() {
        return caseIds;
    }

    public void setCaseIds(List<String> caseIds) {
        this.caseIds = caseIds;
    }

    public List<String> getSourceEvidenceIds() {
        return sourceEvidenceIds;
    }

    public void setSourceEvidenceIds(List<String> sourceEvidenceIds) {
        this.sourceEvidenceIds = sourceEvidenceIds;
    }

    public List<NormalizedEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<NormalizedEventDTO> events) {
        this.events = events;
    }

    public static class Builder {
        private long totalEvents;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<String> caseIds = new ArrayList<>();
        private List<String> sourceEvidenceIds = new ArrayList<>();
        private List<NormalizedEventDTO> events = new ArrayList<>();

        public Builder totalEvents(long totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder caseIds(List<String> caseIds) {
            this.caseIds = caseIds != null ? caseIds : new ArrayList<>();
            return this;
        }

        public Builder sourceEvidenceIds(List<String> sourceEvidenceIds) {
            this.sourceEvidenceIds = sourceEvidenceIds != null ? sourceEvidenceIds : new ArrayList<>();
            return this;
        }

        public Builder events(List<NormalizedEventDTO> events) {
            this.events = events != null ? events : new ArrayList<>();
            return this;
        }

        public TimelineMergeResponse build() {
            return new TimelineMergeResponse(totalEvents, startTime, endTime, caseIds, sourceEvidenceIds, events);
        }
    }
}
