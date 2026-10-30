package com.investigation.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing a normalized digital investigation event.
 * Follows M2 -> M3 data contract.
 */
public class EventModel {
    private String id;
    private String caseId;
    private String evidenceId;
    private Instant timestamp;
    private String source;
    private String eventType;
    private String user;
    private String ipAddress;
    private String device;
    private Boolean success;
    private Long bytesTransferred;
    private String description;

    public EventModel() {
        this.timestamp = Instant.now();
    }

    public EventModel(String id, String caseId, String evidenceId, Instant timestamp,
                      String source, String eventType, String user, String ipAddress,
                      String device, Boolean success, Long bytesTransferred, String description) {
        this.id = id;
        this.caseId = caseId;
        this.evidenceId = evidenceId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.source = source;
        this.eventType = eventType;
        this.user = user;
        this.ipAddress = ipAddress;
        this.device = device;
        this.success = success;
        this.bytesTransferred = bytesTransferred;
        this.description = description;
    }

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

    public Long getBytesTransferred() { return bytesTransferred; }
    public void setBytesTransferred(Long bytesTransferred) { this.bytesTransferred = bytesTransferred; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventModel that = (EventModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
