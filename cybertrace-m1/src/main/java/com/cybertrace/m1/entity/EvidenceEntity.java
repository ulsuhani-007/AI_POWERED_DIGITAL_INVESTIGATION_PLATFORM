package com.cybertrace.m1.entity;

import com.cybertrace.m1.entity.enums.EvidenceStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "evidence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_evidence_case_hash", columnNames = {"case_id", "hash_sha256"})
        },
        indexes = {
                @Index(name = "idx_evidence_case_id", columnList = "case_id"),
                @Index(name = "idx_evidence_hash", columnList = "hash_sha256")
        }
)
public class EvidenceEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseEntity caseEntity;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 64)
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EvidenceStatus status = EvidenceStatus.PENDING;

    @Column(name = "source", length = 128)
    private String source;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_count")
    private Integer eventCount = 0;

    @Column(name = "uploaded_by", length = 64)
    private String uploadedBy;

    public EvidenceEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public EvidenceEntity(String id, CaseEntity caseEntity, String fileName, String fileType,
                          Long fileSizeBytes, String hashSha256, Instant uploadedAt,
                          EvidenceStatus status, String source, String description, Integer eventCount) {
        this(id, caseEntity, fileName, fileType, fileSizeBytes, hashSha256, uploadedAt, status, source, description, eventCount, null);
    }

    public EvidenceEntity(String id, CaseEntity caseEntity, String fileName, String fileType,
                          Long fileSizeBytes, String hashSha256, Instant uploadedAt,
                          EvidenceStatus status, String source, String description, Integer eventCount,
                          String uploadedBy) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.caseEntity = caseEntity;
        this.fileName = fileName;
        this.fileType = fileType != null ? fileType : "CSV Evidence";
        this.fileSizeBytes = fileSizeBytes != null ? fileSizeBytes : 0L;
        this.hashSha256 = hashSha256;
        this.uploadedAt = uploadedAt != null ? uploadedAt : Instant.now();
        this.status = status != null ? status : EvidenceStatus.PENDING;
        this.source = source != null ? source : "Uploaded Evidence";
        this.description = description;
        this.eventCount = eventCount != null ? eventCount : 0;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.uploadedAt == null) {
            this.uploadedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = EvidenceStatus.PENDING;
        }
        if (this.eventCount == null) {
            this.eventCount = 0;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CaseEntity getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(CaseEntity caseEntity) {
        this.caseEntity = caseEntity;
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
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public EvidenceStatus getStatus() {
        return status;
    }

    public void setStatus(EvidenceStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvidenceEntity that = (EvidenceEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

