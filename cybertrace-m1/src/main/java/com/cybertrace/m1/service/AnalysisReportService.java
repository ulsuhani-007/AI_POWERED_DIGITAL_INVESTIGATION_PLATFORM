package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.FindingDTO;
import com.cybertrace.m1.entity.AnalysisReportEntity;
import com.cybertrace.m1.entity.CaseEntity;
import com.cybertrace.m1.repository.AnalysisReportRepository;
import com.cybertrace.m1.repository.NormalizedEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AnalysisReportService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisReportService.class);

    private final AnalysisReportRepository analysisReportRepository;
    private final NormalizedEventRepository normalizedEventRepository;
    private final CaseService caseService;
    private final ObjectMapper objectMapper;
    private final com.cybertrace.m1.client.M3Client m3Client;

    public AnalysisReportService(AnalysisReportRepository analysisReportRepository,
                                 NormalizedEventRepository normalizedEventRepository,
                                 CaseService caseService,
                                 ObjectMapper objectMapper,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) com.cybertrace.m1.client.M3Client m3Client) {
        this.analysisReportRepository = analysisReportRepository;
        this.normalizedEventRepository = normalizedEventRepository;
        this.caseService = caseService;
        this.objectMapper = objectMapper;
        this.m3Client = m3Client;
    }

    @Transactional(readOnly = true)
    public AnalysisDTO getAnalysisByCase(String caseId) {
        CaseEntity caseEntity = caseService.findEntityById(caseId);

        return analysisReportRepository.findByCaseEntity_Id(caseId)
                .map(entity -> mapToDto(entity, caseId))
                .orElseGet(() -> createEmptyAnalysisDto(caseEntity));
    }

    public AnalysisDTO reanalyzeCaseWithM3(String caseId) {
        CaseEntity caseEntity = caseService.findEntityById(caseId);
        List<com.cybertrace.m1.entity.NormalizedEventEntity> eventEntities =
                normalizedEventRepository.findByCaseEntity_IdOrderByTimestampAsc(caseId);

        if (eventEntities.isEmpty()) {
            return getAnalysisByCase(caseId);
        }

        List<com.cybertrace.m1.dto.NormalizedEventDTO> eventDTOs = new ArrayList<>();
        for (com.cybertrace.m1.entity.NormalizedEventEntity e : eventEntities) {
            com.cybertrace.m1.dto.NormalizedEventDTO dto = new com.cybertrace.m1.dto.NormalizedEventDTO();
            dto.setEventId(e.getEventId());
            dto.setId(e.getEventId());
            dto.setCaseId(caseId);
            dto.setTimestamp(e.getTimestamp());
            dto.setEventType(e.getEventType());
            dto.setUser(e.getUser());
            dto.setDevice(e.getDevice());
            dto.setIpAddress(e.getIpAddress());
            dto.setDestinationIp(e.getDestinationIp());
            dto.setFileName(e.getFileName());
            dto.setAction(e.getAction());
            dto.setUrl(e.getUrl());
            dto.setPort(e.getPort());
            dto.setBytes(e.getBytes());
            dto.setSourceEvidenceId(e.getSourceEvidenceId());
            dto.setDescription(e.getDescription());
            dto.setSuccess(e.getSuccess());
            eventDTOs.add(dto);
        }

        if (m3Client != null) {
            try {
                Optional<AnalysisDTO> analysisOpt = m3Client.analyzeEvents(caseId, eventDTOs);
                if (analysisOpt.isPresent()) {
                    return saveAnalysisReport(caseId, analysisOpt.get());
                }
            } catch (Exception ex) {
                log.warn("Live M3 analysis for case {} failed: {}", caseId, ex.getMessage());
            }
        }

        return getAnalysisByCase(caseId);
    }

    public AnalysisDTO saveAnalysisReport(String caseId, AnalysisDTO dto) {
        CaseEntity caseEntity = caseService.findEntityById(caseId);

        String findingsJson = "[]";
        String statsJson = "{}";

        try {
            if (dto.getFindings() != null) {
                findingsJson = objectMapper.writeValueAsString(dto.getFindings());
            }
            if (dto.getStatistics() != null) {
                statsJson = objectMapper.writeValueAsString(dto.getStatistics());
            }
        } catch (Exception e) {
            log.error("Failed to serialize findings or statistics to JSON: {}", e.getMessage(), e);
        }

        AnalysisReportEntity entity = analysisReportRepository.findByCaseEntity_Id(caseId)
                .orElse(new AnalysisReportEntity());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(UUID.randomUUID().toString());
        }
        entity.setCaseEntity(caseEntity);
        entity.setTotalEvents(dto.getTotalEvents() != null ? dto.getTotalEvents() : 0);
        entity.setTotalAnomalies(dto.getTotalAnomalies() != null ? dto.getTotalAnomalies() : 0);
        entity.setFindingsJson(findingsJson);
        entity.setStatisticsJson(statsJson);
        entity.setSummary(dto.getSummary() != null ? dto.getSummary() : "");
        entity.setGeneratedAt(dto.getGeneratedAt() != null ? dto.getGeneratedAt() : Instant.now());

        AnalysisReportEntity saved = analysisReportRepository.save(entity);
        log.info("Persisted analysis report {} for case {} (Total anomalies: {})",
                saved.getId(), caseId, saved.getTotalAnomalies());

        return mapToDto(saved, caseId);
    }

    private AnalysisDTO mapToDto(AnalysisReportEntity entity, String caseId) {
        List<FindingDTO> findings = new ArrayList<>();
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            if (entity.getFindingsJson() != null && !entity.getFindingsJson().isBlank()) {
                findings = objectMapper.readValue(entity.getFindingsJson(), new TypeReference<List<FindingDTO>>() {});
            }
            if (entity.getStatisticsJson() != null && !entity.getStatisticsJson().isBlank()) {
                stats = objectMapper.readValue(entity.getStatisticsJson(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Error parsing persisted JSON findings/statistics for report {}: {}", entity.getId(), e.getMessage());
        }

        return new AnalysisDTO(
                caseId,
                entity.getTotalEvents(),
                entity.getTotalAnomalies(),
                findings,
                stats,
                entity.getSummary(),
                entity.getGeneratedAt()
        );
    }

    private AnalysisDTO createEmptyAnalysisDto(CaseEntity caseEntity) {
        int eventCount = (int) normalizedEventRepository.countByCaseEntity_Id(caseEntity.getId());
        String defaultSummary = String.format(
                "No forensic anomaly detection report has been generated yet for Case %s (%s). Upload evidence files to trigger automated analysis.",
                caseEntity.getCaseNumber(),
                caseEntity.getTitle()
        );

        return new AnalysisDTO(
                caseEntity.getId(),
                eventCount,
                0,
                new ArrayList<>(),
                new LinkedHashMap<>(),
                defaultSummary,
                Instant.now()
        );
    }
}

