package com.investigation.model;

import com.investigation.model.enums.Severity;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing suspicious or anomalous activity detected during analysis.
 * Follows M3 Finding model.
 */
public class FindingModel {
    private String id;
    private String eventId;
    private Instant timestamp;
    private String eventType;
    private String reason;
    private Severity severity;
    private double score; // 0.00 to 1.00
    private String detectionMethod;
    private String affectedEntity; // e.g., user or IP
    private String suggestedAction;

    public FindingModel() {
        this.timestamp = Instant.now();
        this.severity = Severity.MEDIUM;
    }

    public FindingModel(String id, String eventId, Instant timestamp, String eventType,
                        String reason, Severity severity, double score,
                        String detectionMethod, String affectedEntity, String suggestedAction) {
        this.id = id;
        this.eventId = eventId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.eventType = eventType;
        this.reason = reason;
        this.severity = severity != null ? severity : Severity.MEDIUM;
        this.score = score;
        this.detectionMethod = detectionMethod;
        this.affectedEntity = affectedEntity;
        this.suggestedAction = suggestedAction;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }

    public String getAffectedEntity() { return affectedEntity; }
    public void setAffectedEntity(String affectedEntity) { this.affectedEntity = affectedEntity; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FindingModel that = (FindingModel) o;
        return Objects.equals(id, that.id) || (Objects.equals(eventId, that.eventId) && Objects.equals(reason, that.reason));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventId);
    }
}
