package com.investigation.model;

import com.investigation.model.enums.EvidenceStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing an evidence artifact associated with a case.
 */
public class EvidenceModel {
    private String id;
    private String caseId;
    private String fileName;
    private String fileType;
    private long fileSizeBytes;
    private Instant uploadedAt;
    private EvidenceStatus status;
    private String source;
    private String hashSha256;
    private int eventCount;
    private String description;

    public EvidenceModel() {
        this.status = EvidenceStatus.UPLOADED;
        this.uploadedAt = Instant.now();
    }

    public EvidenceModel(String id, String caseId, String fileName, String fileType,
                         long fileSizeBytes, Instant uploadedAt, EvidenceStatus status,
                         String source, String hashSha256, int eventCount, String description) {
        this.id = id;
        this.caseId = caseId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedAt = uploadedAt != null ? uploadedAt : Instant.now();
        this.status = status != null ? status : EvidenceStatus.UPLOADED;
        this.source = source;
        this.hashSha256 = hashSha256;
        this.eventCount = eventCount;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public EvidenceStatus getStatus() { return status; }
    public void setStatus(EvidenceStatus status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFormattedSize() {
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024) return String.format("%.1f KB", fileSizeBytes / 1024.0);
        return String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvidenceModel that = (EvidenceModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
