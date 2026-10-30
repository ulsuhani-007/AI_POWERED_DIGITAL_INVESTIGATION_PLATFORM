package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Data Transfer Object for Event endpoints:
 * - GET /api/cases/{id}/events
 *
 * Implements M2 Canonical Event fields with support for both snake_case and camelCase serialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("caseId")
    @JsonAlias({"case_id"})
    private String caseId;

    @JsonProperty("evidenceId")
    @JsonAlias({"evidence_id", "source_evidence_id"})
    private String evidenceId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("eventType")
    @JsonAlias({"event_type"})
    private String eventType;

    @JsonProperty("user")
    private String user;

    @JsonProperty("ipAddress")
    @JsonAlias({"ip_address", "ip"})
    private String ipAddress;

    @JsonProperty("device")
    private String device;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("bytes")
    @JsonAlias({"bytes_transferred"})
    private Long bytes;

    @JsonProperty("description")
    private String description;

    public EventDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public Long getBytes() { return bytes; }
    public void setBytes(Long bytes) { this.bytes = bytes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
