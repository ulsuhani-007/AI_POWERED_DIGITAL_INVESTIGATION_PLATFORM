package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.UpdateCaseRequest;
import com.cybertrace.m1.entity.CaseEntity;
import com.cybertrace.m1.entity.enums.CasePriority;
import com.cybertrace.m1.entity.enums.CaseStatus;
import com.cybertrace.m1.exception.DuplicateResourceException;
import com.cybertrace.m1.exception.ResourceNotFoundException;
import com.cybertrace.m1.repository.AnalysisReportRepository;
import com.cybertrace.m1.repository.CaseRepository;
import com.cybertrace.m1.repository.EvidenceRepository;
import com.cybertrace.m1.repository.NormalizedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);
    private static final AtomicInteger CASE_SEQ = new AtomicInteger((int) (System.currentTimeMillis() % 9000) + 1000);

    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final NormalizedEventRepository normalizedEventRepository;
    private final AnalysisReportRepository analysisReportRepository;

    public CaseService(CaseRepository caseRepository,
                       EvidenceRepository evidenceRepository,
                       NormalizedEventRepository normalizedEventRepository,
                       AnalysisReportRepository analysisReportRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
        this.normalizedEventRepository = normalizedEventRepository;
        this.analysisReportRepository = analysisReportRepository;
    }

    @Transactional(readOnly = true)
    public List<CaseDTO> getAllCases() {
        List<CaseEntity> entities = caseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<CaseDTO> dtos = new ArrayList<>();
        for (CaseEntity entity : entities) {
            dtos.add(mapToDto(entity));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public CaseDTO getCaseById(String id) {
        CaseEntity entity = findEntityById(id);
        return mapToDto(entity);
    }

    public CaseDTO createCase(CreateCaseRequest request) {
        String caseNumber = request.getCaseNumber();
        if (caseNumber == null || caseNumber.isBlank()) {
            caseNumber = generateCaseNumber();
        } else {
            caseNumber = caseNumber.trim();
            if (caseRepository.existsByCaseNumber(caseNumber)) {
                throw new DuplicateResourceException("Case", "caseNumber", caseNumber);
            }
        }

        CasePriority priority = CasePriority.fromString(request.getPriority());
        String title = request.getTitle() != null ? request.getTitle().trim() : "Untitled Investigation";

        CaseEntity entity = new CaseEntity(
                UUID.randomUUID().toString(),
                caseNumber,
                title,
                request.getDescription(),
                CaseStatus.OPEN,
                priority,
                request.getLeadInvestigator() != null ? request.getLeadInvestigator().trim() : "Unassigned"
        );

        CaseEntity saved = caseRepository.save(entity);
        log.info("Created investigation case {} ({})", saved.getCaseNumber(), saved.getId());
        return mapToDto(saved);
    }

    public CaseDTO updateCase(String id, UpdateCaseRequest request) {
        CaseEntity entity = findEntityById(id);

        if (request.getCaseNumber() != null && !request.getCaseNumber().isBlank()) {
            String newNumber = request.getCaseNumber().trim();
            if (!newNumber.equalsIgnoreCase(entity.getCaseNumber()) && caseRepository.existsByCaseNumber(newNumber)) {
                throw new DuplicateResourceException("Case", "caseNumber", newNumber);
            }
            entity.setCaseNumber(newNumber);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            entity.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(CaseStatus.fromString(request.getStatus()));
        }

        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            entity.setPriority(CasePriority.fromString(request.getPriority()));
        }

        if (request.getLeadInvestigator() != null) {
            entity.setLeadInvestigator(request.getLeadInvestigator().trim());
        }

        CaseEntity saved = caseRepository.save(entity);
        log.info("Updated investigation case {} ({})", saved.getCaseNumber(), saved.getId());
        return mapToDto(saved);
    }

    public void deleteCase(String id) {
        CaseEntity entity = findEntityById(id);
        caseRepository.delete(entity);
        log.info("Deleted investigation case {} ({})", entity.getCaseNumber(), entity.getId());
    }

    @Transactional(readOnly = true)
    public CaseEntity findEntityById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Case ID cannot be null or blank");
        }
        return caseRepository.findById(id.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Case", id));
    }

    private String generateCaseNumber() {
        int year = Year.now().getValue();
        int seq = CASE_SEQ.incrementAndGet();
        String candidate = String.format("CASE-%d-%04d", year, seq % 10000);
        while (caseRepository.existsByCaseNumber(candidate)) {
            seq = CASE_SEQ.incrementAndGet();
            candidate = String.format("CASE-%d-%04d", year, seq % 10000);
        }
        return candidate;
    }

    private CaseDTO mapToDto(CaseEntity entity) {
        int evidenceCount = (int) evidenceRepository.countByCaseEntity_Id(entity.getId());
        int eventCount = (int) normalizedEventRepository.countByCaseEntity_Id(entity.getId());
        int anomalyCount = 0;
        var reportOpt = analysisReportRepository.findByCaseEntity_Id(entity.getId());
        if (reportOpt.isPresent()) {
            anomalyCount = reportOpt.get().getTotalAnomalies();
        }

        return new CaseDTO(
                entity.getId(),
                entity.getCaseNumber(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus() != null ? entity.getStatus().name() : CaseStatus.OPEN.name(),
                entity.getPriority() != null ? entity.getPriority().name() : CasePriority.MEDIUM.name(),
                entity.getLeadInvestigator(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                evidenceCount,
                eventCount,
                anomalyCount
        );
    }
}

