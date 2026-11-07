package com.investigation.api;

import com.investigation.dto.EventDto;
import com.investigation.model.EventModel;
import com.investigation.service.EventService;

import java.util.ArrayList;
import java.util.List;

/**
 * REST implementation for normalized events:
 * - GET /api/cases/{id}/events
 */
public class RestEventService implements EventService {

    private final ApiClient apiClient;

    public RestEventService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<EventModel> getEventsByCase(String caseId) throws Exception {
        List<EventDto> dtos = apiClient.getList("/api/cases/" + caseId + "/events", EventDto.class);
        List<EventModel> models = new ArrayList<>();
        for (EventDto dto : dtos) {
            models.add(mapToModel(dto));
        }
        return models;
    }

    private EventModel mapToModel(EventDto dto) {
        if (dto == null) return null;
        String eventId = dto.getId();
        if (eventId == null || eventId.isBlank()) {
            eventId = java.util.UUID.randomUUID().toString();
        }
        return new EventModel(
                eventId,
                dto.getCaseId(),
                dto.getEvidenceId(),
                dto.getTimestamp() != null ? dto.getTimestamp() : java.time.Instant.now(),
                dto.getSource() != null ? dto.getSource() : "Unknown Source",
                dto.getEventType() != null ? dto.getEventType() : "GENERIC_EVENT",
                dto.getUser(),
                dto.getIpAddress(),
                dto.getDevice(),
                dto.getSuccess(),
                dto.getBytes(),
                dto.getDescription(),
                dto.getDestinationIp(),
                dto.getFileName(),
                dto.getAction(),
                dto.getUrl(),
                dto.getPort()
        );
    }
}
