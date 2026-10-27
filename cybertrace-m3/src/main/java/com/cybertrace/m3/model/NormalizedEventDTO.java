package com.cybertrace.m3.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Normalized evidence event DTO matching the exact M2 schema contract.
 * Non-applicable fields are explicitly serialized as null.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class NormalizedEventDTO {

    @NotBlank(message = "event_id is required")
    @JsonProperty("event_id")
    private String eventId;

    @NotBlank(message = "case_id is required")
    @JsonProperty("case_id")
    private String caseId;

    @NotNull(message = "timestamp is required")
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @NotNull(message = "event_type is required")
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("user")
    private String user;

    @JsonProperty("device")
    private String device;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("destination_ip")
    private String destinationIp;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("action")
    private String action;

    @JsonProperty("url")
    private String url;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("bytes")
    private Long bytes;

    @NotBlank(message = "source_evidence_id is required")
    @JsonProperty("source_evidence_id")
    private String sourceEvidenceId;

    @JsonProperty("description")
    private String description;

    public NormalizedEventDTO() {
    }

    public NormalizedEventDTO(
            String eventId,
            String caseId,
            LocalDateTime timestamp,
            String eventType,
            String user,
            String device,
            String ipAddress,
            String destinationIp,
            String fileName,
            String action,
            String url,
            Integer port,
            Long bytes,
            String sourceEvidenceId,
            String description
    ) {
        this.eventId = eventId;
        this.caseId = caseId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.user = user;
        this.device = device;
        this.ipAddress = ipAddress;
        this.destinationIp = destinationIp;
        this.fileName = fileName;
        this.action = action;
        this.url = url;
        this.port = port;
        this.bytes = bytes;
        this.sourceEvidenceId = sourceEvidenceId;
        this.description = description;
    }

    public static String generateEventId() {
        return "urn:uuid:" + UUID.randomUUID();
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

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public String getSourceEvidenceId() {
        return sourceEvidenceId;
    }

    public void setSourceEvidenceId(String sourceEvidenceId) {
        this.sourceEvidenceId = sourceEvidenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalizedEventDTO that = (NormalizedEventDTO) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(caseId, that.caseId) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(user, that.user) &&
                Objects.equals(device, that.device) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(destinationIp, that.destinationIp) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(action, that.action) &&
                Objects.equals(url, that.url) &&
                Objects.equals(port, that.port) &&
                Objects.equals(bytes, that.bytes) &&
                Objects.equals(sourceEvidenceId, that.sourceEvidenceId) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                eventId, caseId, timestamp, eventType, user, device,
                ipAddress, destinationIp, fileName, action, url,
                port, bytes, sourceEvidenceId, description
        );
    }

    @Override
    public String toString() {
        return "NormalizedEventDTO{" +
                "eventId='" + eventId + '\'' +
                ", caseId='" + caseId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", user='" + user + '\'' +
                ", device='" + device + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", destinationIp='" + destinationIp + '\'' +
                ", fileName='" + fileName + '\'' +
                ", action='" + action + '\'' +
                ", url='" + url + '\'' +
                ", port=" + port +
                ", bytes=" + bytes +
                ", sourceEvidenceId='" + sourceEvidenceId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public static class Builder {
        private String eventId;
        private String caseId;
        private LocalDateTime timestamp;
        private String eventType;
        private String user;
        private String device;
        private String ipAddress;
        private String destinationIp;
        private String fileName;
        private String action;
        private String url;
        private Integer port;
        private Long bytes;
        private String sourceEvidenceId;
        private String description;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder caseId(String caseId) {
            this.caseId = caseId;
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

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder device(String device) {
            this.device = device;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder destinationIp(String destinationIp) {
            this.destinationIp = destinationIp;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder bytes(Long bytes) {
            this.bytes = bytes;
            return this;
        }

        public Builder sourceEvidenceId(String sourceEvidenceId) {
            this.sourceEvidenceId = sourceEvidenceId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public NormalizedEventDTO build() {
            return new NormalizedEventDTO(
                    eventId, caseId, timestamp, eventType, user, device,
                    ipAddress, destinationIp, fileName, action, url,
                    port, bytes, sourceEvidenceId, description
            );
        }
    }
}
