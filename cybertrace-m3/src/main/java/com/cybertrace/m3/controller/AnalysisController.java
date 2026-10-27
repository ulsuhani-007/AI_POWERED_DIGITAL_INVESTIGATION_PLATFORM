package com.cybertrace.m3.controller;

import com.cybertrace.m3.model.AnalysisReportDTO;
import com.cybertrace.m3.model.NormalizedEventDTO;
import com.cybertrace.m3.service.AnalysisService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing endpoints for the M3 forensic analysis engine.
 * Accepts normalized digital evidence events and coordinates anomaly detection,
 * timeline statistics compilation, and forensic investigation summarization.
 */
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Executes forensic analysis over a batch of normalized M2 digital evidence events.
     *
     * @param events list of normalized events from M2
     * @return analysis report containing detected anomalies, metrics, and forensic summary
     */
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisReportDTO> analyze(
            @RequestBody(required = false) List<NormalizedEventDTO> events
    ) {
        AnalysisReportDTO report = analysisService.analyze(events);
        return ResponseEntity.ok(report);
    }
}
