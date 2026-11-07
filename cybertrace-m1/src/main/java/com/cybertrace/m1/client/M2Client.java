package com.cybertrace.m1.client;

import com.cybertrace.m1.dto.M2EvidenceProcessingResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface M2Client {

    /**
     * Forwards an uploaded evidence file to Module 2 for parsing, validation,
     * deduplication, and normalization into standard timeline events.
     *
     * @param caseId case identifier
     * @param sourceEvidenceId source evidence identifier
     * @param file the raw multipart file
     * @return Optional containing M2EvidenceProcessingResponse if successful, empty otherwise
     */
    Optional<M2EvidenceProcessingResponse> processEvidence(String caseId, String sourceEvidenceId, MultipartFile file);
}

