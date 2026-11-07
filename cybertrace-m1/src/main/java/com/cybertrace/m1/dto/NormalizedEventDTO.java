package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class NormalizedEventDTO {

    @JsonProperty("event_id")
    @JsonAlias({"id"})
    private String eventId;

    @JsonProperty("case_id")
    @JsonAlias({"caseId"})
    private String caseId;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("event_type")
    @JsonAlias({"eventType"})
    private String eventType;

    @JsonProperty("user")
    @JsonAlias({"userName", "username"})
    private String user;

    @JsonProperty("device")
    private String device;

    @JsonProperty("ip_address")
    @JsonAlias({"ipAddress", "ip"})
    private String ipAddress;

    @JsonProperty("destination_ip")
    @JsonAlias({"destinationIp"})
    private String destinationIp;

    @JsonProperty("file_name")
    @JsonAlias({"fileName"})
    private String fileName;

    @JsonProperty("action")
    private String action;

    @JsonProperty("url")
    private String url;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("bytes")
    @JsonAlias({"bytes_transferred", "bytesTransferred"})
    private Long bytes;

    @JsonProperty("source_evidence_id")
    @JsonAlias({"sourceEvidenceId", "evidenceId", "evidence_id"})
    private String sourceEvidenceId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("success")
    private Boolean success;

    public NormalizedEventDTO() {}

    public NormalizedEventDTO(String eventId, String caseId, LocalDateTime timestamp,
                              String eventType, String user, String device, String ipAddress,
                              String destinationIp, String fileName, String action, String url,
                              Integer port, Long bytes, String sourceEvidenceId, String description,
                              Boolean success) {
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
        this.success = success;
    }

    // JavaFX EventDto compatibility getter aliases
    @JsonProperty("id")
    public String getId() {
        return eventId;
    }

    @JsonProperty("id")
    public void setId(String id) {
        if (this.eventId == null || this.eventId.isBlank()) {
            this.eventId = id;
        }
    }

    @JsonProperty("caseId")
    public String getCaseIdCamel() {
        return caseId;
    }

    @JsonProperty("eventType")
    public String getEventTypeCamel() {
        return eventType;
    }

    @JsonProperty("ipAddress")
    public String getIpAddressCamel() {
        return ipAddress;
    }

    @JsonProperty("evidenceId")
    public String getEvidenceIdCamel() {
        return sourceEvidenceId;
    }

    // Standard getters and setters
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

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}

