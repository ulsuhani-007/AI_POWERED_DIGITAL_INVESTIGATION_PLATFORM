package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.investigation.util.FlexibleInstantDeserializer;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for Findings/Anomalies within:
 * - GET /api/cases/{id}/analysis
 *
 * Implements M3 Finding model with support for both snake_case and camelCase.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FindingDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("eventId")
    @JsonAlias({"event_id"})
    private String eventId;

    @JsonProperty("timestamp")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant timestamp;

    @JsonProperty("eventType")
    @JsonAlias({"event_type", "anomaly_type", "anomalyType"})
    private String eventType;

    @JsonProperty("anomalyType")
    @JsonAlias({"anomaly_type"})
    private String anomalyType;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("detectionMethod")
    @JsonAlias({"detection_method"})
    private String detectionMethod;

    @JsonProperty("affectedEntity")
    @JsonAlias({"affected_entity", "entity"})
    private String affectedEntity;

    @JsonProperty("suggestedAction")
    @JsonAlias({"suggested_action"})
    private String suggestedAction;

    public FindingDto() {}

    public String getId() {
        if (id != null && !id.isBlank()) return id;
        if (eventId != null && !eventId.isBlank()) return "fnd-" + eventId;
        return UUID.randomUUID().toString();
    }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getEventType() {
        if (eventType != null && !eventType.isBlank()) return eventType;
        if (anomalyType != null && !anomalyType.isBlank()) return anomalyType;
        return "ANOMALY";
    }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Double getScore() { return score != null ? score : 0.0; }
    public void setScore(Double score) { this.score = score; }

    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }

    public String getAffectedEntity() { return affectedEntity; }
    public void setAffectedEntity(String affectedEntity) { this.affectedEntity = affectedEntity; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
}
