package com.cybertrace.m3.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a suspicious forensic finding or anomaly detected during M3 analysis.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AnomalyFindingDTO {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("anomaly_type")
    private AnomalyType anomalyType;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("severity")
    private AnomalySeverity severity;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("detection_method")
    private String detectionMethod;

    public AnomalyFindingDTO() {
    }

    public AnomalyFindingDTO(
            String eventId,
            LocalDateTime timestamp,
            String eventType,
            AnomalyType anomalyType,
            String reason,
            AnomalySeverity severity,
            Double score,
            String detectionMethod
    ) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.anomalyType = anomalyType;
        this.reason = reason;
        this.severity = severity;
        this.score = score;
        this.detectionMethod = detectionMethod;
    }

    public static Builder builder() {
        return new Builder();
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

    public AnomalyType getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(AnomalyType anomalyType) {
        this.anomalyType = anomalyType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AnomalySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AnomalySeverity severity) {
        this.severity = severity;
    }

    public Double getScore() {
        return score;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnomalyFindingDTO that = (AnomalyFindingDTO) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(eventType, that.eventType) &&
                anomalyType == that.anomalyType &&
                Objects.equals(reason, that.reason) &&
                severity == that.severity &&
                Objects.equals(score, that.score) &&
                Objects.equals(detectionMethod, that.detectionMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, timestamp, eventType, anomalyType, reason, severity, score, detectionMethod);
    }

    @Override
    public String toString() {
        return "AnomalyFindingDTO{" +
                "eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", anomalyType=" + anomalyType +
                ", reason='" + reason + '\'' +
                ", severity=" + severity +
                ", score=" + score +
                ", detectionMethod='" + detectionMethod + '\'' +
                '}';
    }

    public static class Builder {
        private String eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private AnomalyType anomalyType;
        private String reason;
        private AnomalySeverity severity;
        private Double score;
        private String detectionMethod;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder anomalyType(AnomalyType anomalyType) {
            this.anomalyType = anomalyType;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder severity(AnomalySeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public Builder detectionMethod(String detectionMethod) {
            this.detectionMethod = detectionMethod;
            return this;
        }

        public AnomalyFindingDTO build() {
            return new AnomalyFindingDTO(
                    eventId, timestamp, eventType, anomalyType,
                    reason, severity, score, detectionMethod
            );
        }
    }
}
