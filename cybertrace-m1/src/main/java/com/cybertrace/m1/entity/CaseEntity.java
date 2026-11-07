package com.cybertrace.m1.entity;

import com.cybertrace.m1.entity.enums.CasePriority;
import com.cybertrace.m1.entity.enums.CaseStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cases", indexes = {
        @Index(name = "idx_cases_case_number", columnList = "case_number", unique = true),
        @Index(name = "idx_cases_status", columnList = "status")
})
public class CaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "case_number", nullable = false, unique = true, length = 64)
    private String caseNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(name = "lead_investigator", length = 128)
    private String leadInvestigator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvidenceEntity> evidence = new ArrayList<>();

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NormalizedEventEntity> events = new ArrayList<>();

    @OneToOne(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AnalysisReportEntity analysisReport;

    public CaseEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public CaseEntity(String id, String caseNumber, String title, String description,
                      CaseStatus status, CasePriority priority, String leadInvestigator) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : CaseStatus.OPEN;
        this.priority = priority != null ? priority : CasePriority.MEDIUM;
        this.leadInvestigator = leadInvestigator;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
        if (this.status == null) {
            this.status = CaseStatus.OPEN;
        }
        if (this.priority == null) {
            this.priority = CasePriority.MEDIUM;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addEvidence(EvidenceEntity ev) {
        if (ev != null) {
            evidence.add(ev);
            ev.setCaseEntity(this);
        }
    }

    public void removeEvidence(EvidenceEntity ev) {
        if (ev != null) {
            evidence.remove(ev);
            ev.setCaseEntity(null);
        }
    }

    public void addEvent(NormalizedEventEntity evt) {
        if (evt != null) {
            events.add(evt);
            evt.setCaseEntity(this);
        }
    }

    public void removeEvent(NormalizedEventEntity evt) {
        if (evt != null) {
            events.remove(evt);
            evt.setCaseEntity(null);
        }
    }

    public void setAnalysisReport(AnalysisReportEntity report) {
        this.analysisReport = report;
        if (report != null) {
            report.setCaseEntity(this);
        }
    }

    // Getters and Setters
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

    public String getName() {
        return title;
    }

    public void setName(String name) {
        this.title = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public void setPriority(CasePriority priority) {
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

    public List<EvidenceEntity> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceEntity> evidence) {
        this.evidence = evidence != null ? evidence : new ArrayList<>();
    }

    public List<NormalizedEventEntity> getEvents() {
        return events;
    }

    public void setEvents(List<NormalizedEventEntity> events) {
        this.events = events != null ? events : new ArrayList<>();
    }

    public AnalysisReportEntity getAnalysisReport() {
        return analysisReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseEntity that = (CaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

