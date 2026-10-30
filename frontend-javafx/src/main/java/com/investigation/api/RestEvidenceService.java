package com.investigation.api;

import com.investigation.dto.EvidenceDto;
import com.investigation.model.EvidenceModel;
import com.investigation.model.enums.EvidenceStatus;
import com.investigation.service.EvidenceService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * REST implementation for evidence upload and retrieval:
 * - GET /api/cases/{id}/evidence
 * - POST /api/cases/{id}/evidence
 */
public class RestEvidenceService implements EvidenceService {

    private final ApiClient apiClient;

    public RestEvidenceService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<EvidenceModel> getEvidenceByCase(String caseId) throws Exception {
        List<EvidenceDto> dtos = apiClient.getList("/api/cases/" + caseId + "/evidence", EvidenceDto.class);
        List<EvidenceModel> models = new ArrayList<>();
        for (EvidenceDto dto : dtos) {
            models.add(mapToModel(dto));
        }
        return models;
    }

    @Override
    public EvidenceModel uploadEvidence(String caseId, File file, String description, Consumer<Double> progressCallback) throws Exception {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist or is invalid.");
        }
        EvidenceDto dto = apiClient.uploadMultipart(
                "/api/cases/" + caseId + "/evidence",
                file,
                description,
                EvidenceDto.class,
                progressCallback
        );
        return mapToModel(dto);
    }

    private EvidenceModel mapToModel(EvidenceDto dto) {
        if (dto == null) return null;
        return new EvidenceModel(
                dto.getId(),
                dto.getCaseId(),
                dto.getFileName(),
                dto.getFileType() != null ? dto.getFileType() : "CSV Evidence",
                dto.getFileSizeBytes() != null ? dto.getFileSizeBytes() : 0L,
                dto.getUploadedAt(),
                EvidenceStatus.fromString(dto.getStatus()),
                dto.getSource() != null ? dto.getSource() : "Uploaded Evidence",
                dto.getHashSha256(),
                dto.getEventCount() != null ? dto.getEventCount() : 0,
                dto.getDescription()
        );
    }
}
