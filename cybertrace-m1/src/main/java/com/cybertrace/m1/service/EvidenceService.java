package com.cybertrace.m1.service;

import com.cybertrace.m1.client.M2Client;
import com.cybertrace.m1.client.M3Client;
import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.EvidenceDTO;
import com.cybertrace.m1.dto.M2EvidenceProcessingResponse;
import com.cybertrace.m1.dto.NormalizedEventDTO;
import com.cybertrace.m1.entity.CaseEntity;
import com.cybertrace.m1.entity.EvidenceEntity;
import com.cybertrace.m1.entity.enums.EvidenceStatus;
import com.cybertrace.m1.exception.DuplicateResourceException;
import com.cybertrace.m1.exception.InvalidInputException;
import com.cybertrace.m1.repository.EvidenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "dll", "msi", "scr", "com", "bin", "pif");

    private final EvidenceRepository evidenceRepository;
    private final CaseService caseService;
    private final Sha256HashService sha256HashService;
    private final M2Client m2Client;
    private final EventService eventService;
    private final M3Client m3Client;
    private final AnalysisReportService analysisReportService;
    private final SecurityAuditService securityAuditService;

    public EvidenceService(EvidenceRepository evidenceRepository,
            CaseService caseService,
            Sha256HashService sha256HashService,
            M2Client m2Client,
            EventService eventService,
            M3Client m3Client,
            AnalysisReportService analysisReportService) {
        this(evidenceRepository, caseService, sha256HashService, m2Client, eventService, m3Client,
                analysisReportService, new SecurityAuditService());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EvidenceService(EvidenceRepository evidenceRepository,
            CaseService caseService,
            Sha256HashService sha256HashService,
            M2Client m2Client,
            EventService eventService,
            M3Client m3Client,
            AnalysisReportService analysisReportService,
            SecurityAuditService securityAuditService) {
        this.evidenceRepository = evidenceRepository;
        this.caseService = caseService;
        this.sha256HashService = sha256HashService;
        this.m2Client = m2Client;
        this.eventService = eventService;
        this.m3Client = m3Client;
        this.analysisReportService = analysisReportService;
        this.securityAuditService = securityAuditService;
    }

    @Transactional(readOnly = true)
    public List<EvidenceDTO> getEvidenceByCase(String caseId) {
        // Verify case exists
        caseService.findEntityById(caseId);

        List<EvidenceEntity> entities = evidenceRepository.findByCaseEntity_IdOrderByUploadedAtDesc(caseId);
        List<EvidenceDTO> dtos = new ArrayList<>();
        for (EvidenceEntity entity : entities) {
            dtos.add(mapToDto(entity));
        }
        return dtos;
    }

    /**
     * Executes the end-to-end evidence ingestion and analysis pipeline:
     * 1. Validate inputs, filename hygiene, and file safety.
     * 2. Calculate cryptographic SHA-256 hash.
     * 3. Enforce case-level duplicate evidence rejection (prevent duplicate
     * ingestion).
     * 4. Record chain of custody metadata (uploadedBy investigator, timestamp,
     * hash).
     * 5. Store initial evidence record (status = PENDING).
     * 6. Forward multipart file to Module 2 (M2) for normalization.
     * 7. Persist returned normalized events with deduplication.
     * 8. Forward cumulative case events to Module 3 (M3) for AI anomaly detection.
     * 9. Persist M3 analysis report.
     * 10. Update evidence status (PROCESSED or FAILED).
     */
    public EvidenceDTO uploadEvidence(String caseId, MultipartFile file, String description) {
        CaseEntity caseEntity = caseService.findEntityById(caseId);

        // 1. File existence and size check
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidInputException("Uploaded evidence file is null or empty");
        }

        String rawFileName = file.getOriginalFilename();
        if (rawFileName == null || rawFileName.isBlank()) {
            rawFileName = "evidence-" + UUID.randomUUID().toString().substring(0, 8) + ".csv";
        }

        // 2. Path traversal sanitization and extension check
        if (rawFileName.contains("..") || rawFileName.contains("/") || rawFileName.contains("\\")) {
            throw new InvalidInputException(
                    "Evidence filename contains illegal path traversal sequences: " + rawFileName);
        }

        String fileName = Paths.get(rawFileName).getFileName().toString();
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx != -1) {
            String extension = fileName.substring(dotIdx + 1).toLowerCase();
            if (FORBIDDEN_EXTENSIONS.contains(extension)) {
                throw new InvalidInputException("Prohibited file extension '." + extension
                        + "'. Executable and script files are forbidden for forensic evidence upload.");
            }
        }

        // 3. Read content bytes safely
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded evidence file: {}", e.getMessage(), e);
            throw new InvalidInputException("Failed to read uploaded file contents: " + e.getMessage(), e);
        }

        if (fileBytes.length == 0) {
            throw new InvalidInputException("Uploaded evidence file contains 0 bytes.");
        }

        // 4. Calculate SHA-256 for forensic integrity and chain of custody
        String sha256 = sha256HashService.calculateHash(fileBytes);
        String fileType = determineFileType(fileName);
        String currentInvestigator = resolveCurrentInvestigator();

        // 5. Enforce Duplicate Evidence Rejection
        if (evidenceRepository.existsByCaseEntity_IdAndHashSha256(caseId, sha256)) {
            securityAuditService.logEvidenceDuplicateRejected(caseId, fileName, sha256, currentInvestigator);
            throw new DuplicateResourceException(
                    "Duplicate evidence detected: A file with identical SHA-256 hash (" + sha256
                            + ") has already been ingested into case " + caseId);
        }

        // 6. Persist initial EvidenceEntity with PENDING status and chain of custody
        EvidenceEntity evidence = new EvidenceEntity(
                UUID.randomUUID().toString(),
                caseEntity,
                fileName,
                fileType,
                file.getSize(),
                sha256,
                Instant.now(),
                EvidenceStatus.PENDING,
                "Uploaded Evidence",
                description,
                0,
                currentInvestigator);

        EvidenceEntity savedEvidence = evidenceRepository.save(evidence);
        log.info("Registered evidence record {} ({}) with SHA-256 {} by '{}' for case {}",
                savedEvidence.getFileName(), savedEvidence.getId(), savedEvidence.getHashSha256(), currentInvestigator,
                caseId);
        securityAuditService.logEvidenceUploaded(caseId, savedEvidence.getId(), fileName, sha256, currentInvestigator);

        // 7. Forward to M2 for evidence processing and normalization
        Optional<M2EvidenceProcessingResponse> m2ResponseOpt = m2Client.processEvidence(caseId, savedEvidence.getId(),
                file);

        if (m2ResponseOpt.isPresent()) {
            M2EvidenceProcessingResponse m2Response = m2ResponseOpt.get();
            List<NormalizedEventDTO> normalizedEvents = m2Response.getEvents();

            log.info("M2 returned {} events for evidence {}", normalizedEvents.size(), savedEvidence.getId());

            // 8. Persist normalized events into database
            if (!normalizedEvents.isEmpty()) {
                List<NormalizedEventDTO> persistedEvents = eventService.ingestEvents(caseId, normalizedEvents);
                log.info("Persisted {} new events for case {}", persistedEvents.size(), caseId);
            }

            // Update evidence status to PROCESSED
            savedEvidence.setStatus(EvidenceStatus.PROCESSED);
            savedEvidence.setEventCount(normalizedEvents.size());
            savedEvidence = evidenceRepository.save(savedEvidence);
            securityAuditService.logEvidenceProcessed(savedEvidence.getId(), normalizedEvents.size(), "PROCESSED");

            // Advance case status to IN_PROGRESS as evidence has been ingested and normalized
            caseService.updateCaseStatus(caseId, com.cybertrace.m1.entity.enums.CaseStatus.IN_PROGRESS);

            // 9. Trigger M3 Analysis with all cumulative case events
            try {
                List<NormalizedEventDTO> allCaseEvents = eventService.getEventsByCase(caseId);
                if (!allCaseEvents.isEmpty()) {
                    log.info("Triggering M3 forensic analysis for case {} with {} cumulative events", caseId,
                            allCaseEvents.size());
                    Optional<AnalysisDTO> m3AnalysisOpt = m3Client.analyzeEvents(caseId, allCaseEvents);

                    if (m3AnalysisOpt.isPresent()) {
                        AnalysisDTO analysisDto = m3AnalysisOpt.get();
                        analysisReportService.saveAnalysisReport(caseId, analysisDto);
                        log.info("Successfully persisted M3 analysis report for case {}: {} anomalies detected",
                                caseId, analysisDto.getTotalAnomalies());
                        securityAuditService.logAnalysisExecuted(caseId, allCaseEvents.size(),
                                analysisDto.getTotalAnomalies());
                    } else {
                        log.warn(
                                "M3 analysis was unavailable or returned empty for case {}. Cumulative events remain saved.",
                                caseId);
                    }
                }
            } catch (Exception e) {
                log.error("Non-fatal error during M3 analysis for case {}: {}", caseId, e.getMessage(), e);
            }

        } else {
            // M2 processing failed or was unavailable
            log.warn("Evidence normalization failed or schema unsupported for evidence {}. Marking status as FAILED.",
                    savedEvidence.getId());
            savedEvidence.setStatus(EvidenceStatus.FAILED);
            String failureNote = "[Evidence schema unrecognized or event normalization failed. File preserved with SHA-256 for chain of custody.]";
            String updatedDesc = (description != null && !description.isBlank())
                    ? description + " " + failureNote
                    : failureNote;
            savedEvidence.setDescription(updatedDesc);
            savedEvidence = evidenceRepository.save(savedEvidence);
            securityAuditService.logEvidenceProcessed(savedEvidence.getId(), 0, "FAILED");
        }

        return mapToDto(savedEvidence);
    }

    private String resolveCurrentInvestigator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof com.cybertrace.m1.entity.UserEntity user) {
                return user.getUsername();
            }
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                return ud.getUsername();
            }
            String name = auth.getName();
            if (name != null && !"anonymousUser".equals(name)) {
                return name.length() > 64 ? name.substring(0, 64) : name;
            }
        }
        return "investigator";
    }

    private String determineFileType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("login") || lower.contains("auth")) {
            return "LOGIN_LOG";
        } else if (lower.contains("browser") || lower.contains("history")) {
            return "BROWSER_HISTORY";
        } else if (lower.contains("file") || lower.contains("audit")) {
            return "FILE_AUDIT";
        } else if (lower.contains("network") || lower.contains("pcap") || lower.contains("flow")) {
            return "NETWORK_CAPTURE";
        }
        return "CSV Evidence";
    }

    public void deleteEvidence(String caseId, String evidenceId) {
        EvidenceEntity entity = evidenceRepository.findByIdAndCaseEntity_Id(evidenceId, caseId)
                .orElseThrow(() -> new com.cybertrace.m1.exception.ResourceNotFoundException("Evidence", evidenceId));

        eventService.deleteEventsByEvidenceId(evidenceId);
        evidenceRepository.delete(entity);
        log.info("Deleted evidence {} from case {}", evidenceId, caseId);
    }

    private EvidenceDTO mapToDto(EvidenceEntity entity) {
        return new EvidenceDTO(
                entity.getId(),
                entity.getCaseEntity() != null ? entity.getCaseEntity().getId() : null,
                entity.getFileName(),
                entity.getFileType(),
                entity.getFileSizeBytes(),
                entity.getUploadedAt(),
                entity.getStatus() != null ? entity.getStatus().name() : EvidenceStatus.PENDING.name(),
                entity.getSource(),
                entity.getHashSha256(),
                entity.getEventCount(),
                entity.getDescription(),
                entity.getUploadedBy());
    }
}
