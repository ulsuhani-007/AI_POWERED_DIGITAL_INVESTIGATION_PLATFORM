package com.cybertrace.m1.client;

import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.NormalizedEventDTO;

import java.util.List;
import java.util.Optional;

public interface M3Client {

    /**
     * Sends a case's chronological normalized events to Module 3 for multi-vector
     * anomaly detection (Brute Force, Network Volume MAD, Off-Hours, and Isolation Forest ML),
     * baseline investigation statistics, and grounded forensic summary generation.
     *
     * @param caseId case identifier
     * @param events list of normalized events to analyze
     * @return Optional containing AnalysisDTO if analysis succeeds, empty otherwise
     */
    Optional<AnalysisDTO> analyzeEvents(String caseId, List<NormalizedEventDTO> events);
}

