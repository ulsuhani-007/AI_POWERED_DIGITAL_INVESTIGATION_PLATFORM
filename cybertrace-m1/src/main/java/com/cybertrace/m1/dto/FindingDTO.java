package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindingDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("event_id")
    @JsonAlias({"eventId"})
    private String eventId;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("event_type")
    @JsonAlias({"eventType"})
    private String eventType;

    @JsonProperty("anomaly_type")
    @JsonAlias({"anomalyType"})
    private String anomalyType;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("detection_method")
    @JsonAlias({"detectionMethod"})
    private String detectionMethod;

    @JsonProperty("affected_entity")
    @JsonAlias({"affectedEntity", "entity"})
    private String affectedEntity;

    @JsonProperty("suggested_action")
    @JsonAlias({"suggestedAction"})
    private String suggestedAction;

    public FindingDTO() {
        this.id = UUID.randomUUID().toString();
    }

    public FindingDTO(String id, String eventId, LocalDateTime timestamp, String eventType,
                      String anomalyType, String reason, String severity, Double score,
                      String detectionMethod, String affectedEntity, String suggestedAction) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.anomalyType = anomalyType;
        this.reason = reason;
        this.severity = severity;
        this.score = score != null ? score : 0.0;
        this.detectionMethod = detectionMethod;
        this.affectedEntity = affectedEntity;
        this.suggestedAction = suggestedAction;
    }

    // JavaFX camelCase compatibility getter aliases
    @JsonProperty("eventId")
    public String getEventIdCamel() {
        return eventId;
    }

    @JsonProperty("eventType")
    public String getEventTypeCamel() {
        return eventType;
    }

    @JsonProperty("detectionMethod")
    public String getDetectionMethodCamel() {
        return detectionMethod;
    }

    @JsonProperty("affectedEntity")
    public String getAffectedEntityCamel() {
        return affectedEntity;
    }

    @JsonProperty("suggestedAction")
    public String getSuggestedActionCamel() {
        return suggestedAction;
    }

    // Standard getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(String anomalyType) {
        this.anomalyType = anomalyType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Double getScore() {
        return score != null ? score : 0.0;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public String getAffectedEntity() {
        return affectedEntity;
    }

    public void setAffectedEntity(String affectedEntity) {
        this.affectedEntity = affectedEntity;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
}

