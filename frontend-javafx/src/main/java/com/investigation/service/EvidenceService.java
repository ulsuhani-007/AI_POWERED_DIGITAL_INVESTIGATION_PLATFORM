package com.investigation.service;

import com.investigation.model.EvidenceModel;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface EvidenceService {
    List<EvidenceModel> getEvidenceByCase(String caseId) throws Exception;
    EvidenceModel uploadEvidence(String caseId, File file, String description, Consumer<Double> progressCallback) throws Exception;
}
