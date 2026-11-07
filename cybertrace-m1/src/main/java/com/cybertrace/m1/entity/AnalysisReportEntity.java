package com.cybertrace.m1.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "analysis_reports", indexes = {
        @Index(name = "idx_analysis_case_id", columnList = "case_id", unique = true)
})
public class AnalysisReportEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private CaseEntity caseEntity;

    @Column(name = "total_events", nullable = false)
    private Integer totalEvents = 0;

    @Column(name = "total_anomalies", nullable = false)
    private Integer totalAnomalies = 0;

    @Column(name = "findings_json", columnDefinition = "TEXT")
    private String findingsJson;

    @Column(name = "statistics_json", columnDefinition = "TEXT")
    private String statisticsJson;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public AnalysisReportEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public AnalysisReportEntity(String id, CaseEntity caseEntity, Integer totalEvents,
                                Integer totalAnomalies, String findingsJson,
                                String statisticsJson, String summary, Instant generatedAt) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.caseEntity = caseEntity;
        this.totalEvents = totalEvents != null ? totalEvents : 0;
        this.totalAnomalies = totalAnomalies != null ? totalAnomalies : 0;
        this.findingsJson = findingsJson;
        this.statisticsJson = statisticsJson;
        this.summary = summary;
        this.generatedAt = generatedAt != null ? generatedAt : Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
        }
        if (this.totalEvents == null) {
            this.totalEvents = 0;
        }
        if (this.totalAnomalies == null) {
            this.totalAnomalies = 0;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CaseEntity getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(CaseEntity caseEntity) {
        this.caseEntity = caseEntity;
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

    public String getFindingsJson() {
        return findingsJson;
    }

    public void setFindingsJson(String findingsJson) {
        this.findingsJson = findingsJson;
    }

    public String getStatisticsJson() {
        return statisticsJson;
    }

    public void setStatisticsJson(String statisticsJson) {
        this.statisticsJson = statisticsJson;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisReportEntity that = (AnalysisReportEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

