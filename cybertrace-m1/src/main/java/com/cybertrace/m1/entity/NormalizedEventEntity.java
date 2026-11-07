package com.cybertrace.m1.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "normalized_events", indexes = {
        @Index(name = "idx_events_case_id", columnList = "case_id"),
        @Index(name = "idx_events_timestamp", columnList = "timestamp"),
        @Index(name = "idx_events_type", columnList = "event_type"),
        @Index(name = "idx_events_source_evidence", columnList = "source_evidence_id")
})
public class NormalizedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseEntity caseEntity;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "user_name", length = 128)
    private String user;

    @Column(name = "device", length = 128)
    private String device;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "destination_ip", length = 64)
    private String destinationIp;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "action", length = 128)
    private String action;

    @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "port")
    private Integer port;

    @Column(name = "bytes")
    private Long bytes;

    @Column(name = "source_evidence_id", length = 128)
    private String sourceEvidenceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "success")
    private Boolean success;

    public NormalizedEventEntity() {
        this.eventId = "urn:uuid:" + UUID.randomUUID().toString();
    }

    public NormalizedEventEntity(String eventId, CaseEntity caseEntity, LocalDateTime timestamp,
                                 String eventType, String user, String device, String ipAddress,
                                 String destinationIp, String fileName, String action, String url,
                                 Integer port, Long bytes, String sourceEvidenceId, String description,
                                 Boolean success) {
        this.eventId = (eventId != null && !eventId.isBlank()) ? eventId : "urn:uuid:" + UUID.randomUUID().toString();
        this.caseEntity = caseEntity;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
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

    @PrePersist
    public void prePersist() {
        if (this.eventId == null || this.eventId.isBlank()) {
            this.eventId = "urn:uuid:" + UUID.randomUUID().toString();
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public CaseEntity getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(CaseEntity caseEntity) {
        this.caseEntity = caseEntity;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalizedEventEntity that = (NormalizedEventEntity) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}

