package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.service.AnalysisReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases/{caseId}/analysis")
@CrossOrigin(origins = "*")
public class AnalysisReportController {

    private final AnalysisReportService analysisReportService;

    public AnalysisReportController(AnalysisReportService analysisReportService) {
        this.analysisReportService = analysisReportService;
    }

    @GetMapping
    public ResponseEntity<AnalysisDTO> getAnalysisByCase(@PathVariable("caseId") String caseId) {
        AnalysisDTO report = analysisReportService.getAnalysisByCase(caseId);
        return ResponseEntity.ok(report);
    }

    @PostMapping
    public ResponseEntity<AnalysisDTO> saveAnalysisReport(
            @PathVariable("caseId") String caseId,
            @RequestBody AnalysisDTO analysisDTO) {
        AnalysisDTO saved = analysisReportService.saveAnalysisReport(caseId, analysisDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/reanalyze")
    public ResponseEntity<AnalysisDTO> reanalyzeCase(@PathVariable("caseId") String caseId) {
        AnalysisDTO report = analysisReportService.reanalyzeCaseWithM3(caseId);
        return ResponseEntity.ok(report);
    }
}

