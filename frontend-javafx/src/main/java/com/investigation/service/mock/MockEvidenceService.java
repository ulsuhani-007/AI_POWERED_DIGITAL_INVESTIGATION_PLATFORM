package com.investigation.service.mock;

import com.investigation.model.EvidenceModel;
import com.investigation.model.enums.EvidenceStatus;
import com.investigation.service.EvidenceService;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MockEvidenceService implements EvidenceService {

    private final MockDataGenerator generator;

    public MockEvidenceService(MockDataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public List<EvidenceModel> getEvidenceByCase(String caseId) {
        return generator.getEvidenceByCase(caseId);
    }

    @Override
    public EvidenceModel uploadEvidence(String caseId, File file, String description, Consumer<Double> progressCallback) throws Exception {
        // Simulate file upload progress in steps
        for (int i = 1; i <= 10; i++) {
            Thread.sleep(100);
            if (progressCallback != null) {
                progressCallback.accept(i / 10.0);
            }
        }

        String id = "ev-" + UUID.randomUUID().toString().substring(0, 8);
        String fileName = file != null ? file.getName() : "uploaded_evidence.csv";
        long size = file != null ? file.length() : 1024L;

        EvidenceModel evidence = new EvidenceModel(
                id,
                caseId,
                fileName,
                "CSV / Delimited Log [SIMULATED]",
                size,
                Instant.now(),
                EvidenceStatus.ANALYZED,
                "Local Examiner Ingest",
                "simulated_sha256_" + System.currentTimeMillis(),
                150,
                description != null && !description.isBlank() ? description : "Simulated uploaded evidence file."
        );

        generator.addEvidence(caseId, evidence);
        return evidence;
    }
}
