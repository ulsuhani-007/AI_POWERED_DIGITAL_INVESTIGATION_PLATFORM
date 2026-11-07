package com.cybertrace.m1.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvidenceDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("caseId")
    @JsonAlias({"case_id"})
    private String caseId;

    @JsonProperty("fileName")
    @JsonAlias({"file_name"})
    private String fileName;

    @JsonProperty("fileType")
    @JsonAlias({"file_type"})
    private String fileType;

    @JsonProperty("fileSizeBytes")
    @JsonAlias({"file_size_bytes", "fileSize", "file_size"})
    private Long fileSizeBytes;

    @JsonProperty("uploadedAt")
    @JsonAlias({"uploaded_at"})
    private Instant uploadedAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("source")
    private String source;

    @JsonProperty("hashSha256")
    @JsonAlias({"hash_sha256", "sha256", "sha256Hash"})
    private String hashSha256;

    @JsonProperty("eventCount")
    @JsonAlias({"event_count"})
    private Integer eventCount = 0;

    @JsonProperty("description")
    private String description;

    @JsonProperty("uploadedBy")
    @JsonAlias({"uploaded_by"})
    private String uploadedBy;

    public EvidenceDTO() {}

    public EvidenceDTO(String id, String caseId, String fileName, String fileType,
                       Long fileSizeBytes, Instant uploadedAt, String status,
                       String source, String hashSha256, Integer eventCount, String description) {
        this(id, caseId, fileName, fileType, fileSizeBytes, uploadedAt, status, source, hashSha256, eventCount, description, null);
    }

    public EvidenceDTO(String id, String caseId, String fileName, String fileType,
                       Long fileSizeBytes, Instant uploadedAt, String status,
                       String source, String hashSha256, Integer eventCount, String description,
                       String uploadedBy) {
        this.id = id;
        this.caseId = caseId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes != null ? fileSizeBytes : 0L;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.source = source;
        this.hashSha256 = hashSha256;
        this.eventCount = eventCount != null ? eventCount : 0;
        this.description = description;
        this.uploadedBy = uploadedBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes != null ? fileSizeBytes : 0L;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public Integer getEventCount() {
        return eventCount != null ? eventCount : 0;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}

