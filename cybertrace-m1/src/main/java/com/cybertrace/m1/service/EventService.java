package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.NormalizedEventDTO;
import com.cybertrace.m1.entity.CaseEntity;
import com.cybertrace.m1.entity.NormalizedEventEntity;
import com.cybertrace.m1.repository.NormalizedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final NormalizedEventRepository normalizedEventRepository;
    private final CaseService caseService;

    public EventService(NormalizedEventRepository normalizedEventRepository,
                        CaseService caseService) {
        this.normalizedEventRepository = normalizedEventRepository;
        this.caseService = caseService;
    }

    @Transactional(readOnly = true)
    public List<NormalizedEventDTO> getEventsByCase(String caseId) {
        // Verify case exists
        caseService.findEntityById(caseId);

        List<NormalizedEventEntity> entities = normalizedEventRepository.findByCaseEntity_IdOrderByTimestampAsc(caseId);
        List<NormalizedEventDTO> dtos = new ArrayList<>();
        for (NormalizedEventEntity entity : entities) {
            dtos.add(mapToDto(entity));
        }
        return dtos;
    }

    public List<NormalizedEventDTO> ingestEvents(String caseId, List<NormalizedEventDTO> eventDTOs) {
        CaseEntity caseEntity = caseService.findEntityById(caseId);

        if (eventDTOs == null || eventDTOs.isEmpty()) {
            return List.of();
        }

        Set<String> seenInBatch = new HashSet<>();
        List<NormalizedEventEntity> entitiesToSave = new ArrayList<>();

        for (NormalizedEventDTO dto : eventDTOs) {
            String eventId = dto.getEventId();
            if (eventId == null || eventId.isBlank()) {
                eventId = dto.getId();
            }
            if (eventId == null || eventId.isBlank()) {
                eventId = "urn:uuid:" + UUID.randomUUID().toString();
            }

            // Prevent duplicate event insertion in same batch
            if (!seenInBatch.add(eventId)) {
                log.debug("Event ID {} already seen in current batch, skipping duplicate", eventId);
                continue;
            }

            // Prevent duplicate event insertion if already in database
            if (normalizedEventRepository.existsById(eventId)) {
                log.debug("Event ID {} already persisted in database, skipping duplicate", eventId);
                continue;
            }

            Boolean success = dto.getSuccess();
            if (success == null && dto.getAction() != null) {
                String actionUpper = dto.getAction().toUpperCase();
                if (actionUpper.contains("SUCCESS")) {
                    success = true;
                } else if (actionUpper.contains("FAIL") || actionUpper.contains("DENIED")) {
                    success = false;
                }
            }

            NormalizedEventEntity entity = new NormalizedEventEntity(
                    eventId,
                    caseEntity,
                    dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now(),
                    dto.getEventType() != null ? dto.getEventType() : "GENERIC_EVENT",
                    dto.getUser(),
                    dto.getDevice(),
                    dto.getIpAddress(),
                    dto.getDestinationIp(),
                    dto.getFileName(),
                    dto.getAction(),
                    dto.getUrl(),
                    dto.getPort(),
                    dto.getBytes(),
                    dto.getSourceEvidenceId(),
                    dto.getDescription(),
                    success
            );
            entitiesToSave.add(entity);
        }

        List<NormalizedEventEntity> savedEntities = normalizedEventRepository.saveAll(entitiesToSave);
        log.info("Persisted {} normalized events for case {} (filtered out {} duplicates)",
                savedEntities.size(), caseId, eventDTOs.size() - savedEntities.size());

        List<NormalizedEventDTO> result = new ArrayList<>();
        for (NormalizedEventEntity entity : savedEntities) {
            result.add(mapToDto(entity));
        }
        return result;
    }

    private NormalizedEventDTO mapToDto(NormalizedEventEntity entity) {
        return new NormalizedEventDTO(
                entity.getEventId(),
                entity.getCaseEntity() != null ? entity.getCaseEntity().getId() : null,
                entity.getTimestamp(),
                entity.getEventType(),
                entity.getUser(),
                entity.getDevice(),
                entity.getIpAddress(),
                entity.getDestinationIp(),
                entity.getFileName(),
                entity.getAction(),
                entity.getUrl(),
                entity.getPort(),
                entity.getBytes(),
                entity.getSourceEvidenceId(),
                entity.getDescription(),
                entity.getSuccess()
        );
    }
}
