package com.investigation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Data Transfer Object for Evidence endpoints:
 * - GET /api/cases/{id}/evidence
 * - POST /api/cases/{id}/evidence
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvidenceDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("caseId")
    private String caseId;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileType")
    private String fileType;

    @JsonProperty("fileSizeBytes")
    private Long fileSizeBytes;

    @JsonProperty("uploadedAt")
    private Instant uploadedAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("source")
    private String source;

    @JsonProperty("hashSha256")
    private String hashSha256;

    @JsonProperty("eventCount")
    private Integer eventCount;

    @JsonProperty("description")
    private String description;

    public EvidenceDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSizeBytes() { return fileSizeBytes != null ? fileSizeBytes : 0L; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }

    public Integer getEventCount() { return eventCount != null ? eventCount : 0; }
    public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
