package com.investigation.model;

import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing an investigation case.
 */
public class CaseModel {
    private String id;
    private String caseNumber;
    private String title;
    private String description;
    private CaseStatus status;
    private CasePriority priority;
    private String leadInvestigator;
    private Instant createdAt;
    private Instant updatedAt;
    private int evidenceCount;
    private int eventCount;
    private int anomalyCount;

    public CaseModel() {
        this.status = CaseStatus.OPEN;
        this.priority = CasePriority.MEDIUM;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public CaseModel(String id, String caseNumber, String title, String description,
                     CaseStatus status, CasePriority priority, String leadInvestigator,
                     Instant createdAt, Instant updatedAt,
                     int evidenceCount, int eventCount, int anomalyCount) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : CaseStatus.OPEN;
        this.priority = priority != null ? priority : CasePriority.MEDIUM;
        this.leadInvestigator = leadInvestigator;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.evidenceCount = evidenceCount;
        this.eventCount = eventCount;
        this.anomalyCount = anomalyCount;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    public CasePriority getPriority() { return priority; }
    public void setPriority(CasePriority priority) { this.priority = priority; }

    public String getLeadInvestigator() { return leadInvestigator; }
    public void setLeadInvestigator(String leadInvestigator) { this.leadInvestigator = leadInvestigator; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public int getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(int evidenceCount) { this.evidenceCount = evidenceCount; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    public int getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(int anomalyCount) { this.anomalyCount = anomalyCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseModel caseModel = (CaseModel) o;
        return Objects.equals(id, caseModel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (caseNumber != null ? caseNumber + ": " : "") + (title != null ? title : "Untitled Case");
    }
}
