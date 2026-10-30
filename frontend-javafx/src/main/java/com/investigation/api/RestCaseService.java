package com.investigation.api;

import com.investigation.dto.CaseDto;
import com.investigation.dto.CreateCaseRequest;
import com.investigation.model.CaseModel;
import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;
import com.investigation.service.CaseService;

import java.util.ArrayList;
import java.util.List;

public class RestCaseService implements CaseService {

    private final ApiClient apiClient;

    public RestCaseService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<CaseModel> getAllCases() throws Exception {
        List<CaseDto> dtos = apiClient.getList("/api/cases", CaseDto.class);
        List<CaseModel> models = new ArrayList<>();
        for (CaseDto dto : dtos) {
            models.add(mapToModel(dto));
        }
        return models;
    }

    @Override
    public CaseModel getCaseById(String id) throws Exception {
        CaseDto dto = apiClient.get("/api/cases/" + id, CaseDto.class);
        return mapToModel(dto);
    }

    @Override
    public CaseModel createCase(CaseModel caseModel) throws Exception {
        CreateCaseRequest req = new CreateCaseRequest(
                caseModel.getCaseNumber(),
                caseModel.getTitle(),
                caseModel.getDescription(),
                caseModel.getPriority() != null ? caseModel.getPriority().name() : "MEDIUM",
                caseModel.getLeadInvestigator()
        );
        CaseDto dto = apiClient.post("/api/cases", req, CaseDto.class);
        return mapToModel(dto);
    }

    @Override
    public CaseModel updateCase(CaseModel caseModel) throws Exception {
        CaseDto dto = apiClient.put("/api/cases/" + caseModel.getId(), mapToDto(caseModel), CaseDto.class);
        return mapToModel(dto);
    }

    @Override
    public boolean deleteCase(String id) throws Exception {
        return apiClient.delete("/api/cases/" + id);
    }

    private CaseModel mapToModel(CaseDto dto) {
        if (dto == null) return null;
        return new CaseModel(
                dto.getId(),
                dto.getCaseNumber(),
                dto.getTitle(),
                dto.getDescription(),
                CaseStatus.fromString(dto.getStatus()),
                CasePriority.fromString(dto.getPriority()),
                dto.getLeadInvestigator(),
                dto.getCreatedAt(),
                dto.getUpdatedAt(),
                dto.getEvidenceCount(),
                dto.getEventCount(),
                dto.getAnomalyCount()
        );
    }

    private CaseDto mapToDto(CaseModel model) {
        if (model == null) return null;
        CaseDto dto = new CaseDto();
        dto.setId(model.getId());
        dto.setCaseNumber(model.getCaseNumber());
        dto.setTitle(model.getTitle());
        dto.setDescription(model.getDescription());
        dto.setStatus(model.getStatus() != null ? model.getStatus().name() : "OPEN");
        dto.setPriority(model.getPriority() != null ? model.getPriority().name() : "MEDIUM");
        dto.setLeadInvestigator(model.getLeadInvestigator());
        dto.setCreatedAt(model.getCreatedAt());
        dto.setUpdatedAt(model.getUpdatedAt());
        dto.setEvidenceCount(model.getEvidenceCount());
        dto.setEventCount(model.getEventCount());
        dto.setAnomalyCount(model.getAnomalyCount());
        return dto;
    }
}
