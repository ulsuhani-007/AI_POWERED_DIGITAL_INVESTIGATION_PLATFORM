package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Data Transfer Object for Case endpoints:
 * - GET /api/cases
 * - GET /api/cases/{id}
 * - PUT /api/cases/{id}
 *
 * Assumed contract fields based on standard Spring Boot/PostgreSQL schema.
 * Tolerant to extra/missing fields via @JsonIgnoreProperties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("caseNumber")
    private String caseNumber;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("leadInvestigator")
    private String leadInvestigator;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("evidenceCount")
    private Integer evidenceCount;

    @JsonProperty("eventCount")
    private Integer eventCount;

    @JsonProperty("anomalyCount")
    private Integer anomalyCount;

    public CaseDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getLeadInvestigator() { return leadInvestigator; }
    public void setLeadInvestigator(String leadInvestigator) { this.leadInvestigator = leadInvestigator; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getEvidenceCount() { return evidenceCount != null ? evidenceCount : 0; }
    public void setEvidenceCount(Integer evidenceCount) { this.evidenceCount = evidenceCount; }

    public Integer getEventCount() { return eventCount != null ? eventCount : 0; }
    public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }

    public Integer getAnomalyCount() { return anomalyCount != null ? anomalyCount : 0; }
    public void setAnomalyCount(Integer anomalyCount) { this.anomalyCount = anomalyCount; }
}
