package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaseDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("caseNumber")
    @JsonAlias({"case_number"})
    private String caseNumber;

    @JsonProperty("title")
    @JsonAlias({"name"})
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("leadInvestigator")
    @JsonAlias({"lead_investigator"})
    private String leadInvestigator;

    @JsonProperty("createdAt")
    @JsonAlias({"created_at"})
    private Instant createdAt;

    @JsonProperty("updatedAt")
    @JsonAlias({"updated_at"})
    private Instant updatedAt;

    @JsonProperty("evidenceCount")
    @JsonAlias({"evidence_count"})
    private Integer evidenceCount = 0;

    @JsonProperty("eventCount")
    @JsonAlias({"event_count"})
    private Integer eventCount = 0;

    @JsonProperty("anomalyCount")
    @JsonAlias({"anomaly_count"})
    private Integer anomalyCount = 0;

    public CaseDTO() {}

    public CaseDTO(String id, String caseNumber, String title, String description,
                   String status, String priority, String leadInvestigator,
                   Instant createdAt, Instant updatedAt,
                   Integer evidenceCount, Integer eventCount, Integer anomalyCount) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.leadInvestigator = leadInvestigator;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.evidenceCount = evidenceCount != null ? evidenceCount : 0;
        this.eventCount = eventCount != null ? eventCount : 0;
        this.anomalyCount = anomalyCount != null ? anomalyCount : 0;
    }

    @JsonProperty("name")
    public String getName() {
        return title;
    }

    @JsonProperty("name")
    public void setName(String name) {
        if (this.title == null || this.title.isBlank()) {
            this.title = name;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLeadInvestigator() {
        return leadInvestigator;
    }

    public void setLeadInvestigator(String leadInvestigator) {
        this.leadInvestigator = leadInvestigator;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getEvidenceCount() {
        return evidenceCount != null ? evidenceCount : 0;
    }

    public void setEvidenceCount(Integer evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public Integer getEventCount() {
        return eventCount != null ? eventCount : 0;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public Integer getAnomalyCount() {
        return anomalyCount != null ? anomalyCount : 0;
    }

    public void setAnomalyCount(Integer anomalyCount) {
        this.anomalyCount = anomalyCount;
    }
}

