package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.investigation.util.FlexibleInstantDeserializer;

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
    @JsonAlias({"event_id", "eventId"})
    private String id;

    @JsonProperty("caseId")
    @JsonAlias({"case_id"})
    private String caseId;

    @JsonProperty("evidenceId")
    @JsonAlias({"evidence_id", "source_evidence_id", "sourceEvidenceId"})
    private String evidenceId;

    @JsonProperty("timestamp")
    @JsonDeserialize(using = FlexibleInstantDeserializer.class)
    private Instant timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("eventType")
    @JsonAlias({"event_type"})
    private String eventType;

    @JsonProperty("user")
    private String user;

    @JsonProperty("ipAddress")
    @JsonAlias({"ip_address", "ip", "source_ip", "sourceIp"})
    private String ipAddress;

    @JsonProperty("device")
    private String device;

    @JsonProperty("destinationIp")
    @JsonAlias({"destination_ip", "destination", "destIp", "dest_ip"})
    private String destinationIp;

    @JsonProperty("fileName")
    @JsonAlias({"file_name", "filename", "file"})
    private String fileName;

    @JsonProperty("action")
    private String action;

    @JsonProperty("url")
    private String url;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("bytes")
    @JsonAlias({"bytes_transferred", "bytesTransferred"})
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

    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String destinationIp) { this.destinationIp = destinationIp; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
}
