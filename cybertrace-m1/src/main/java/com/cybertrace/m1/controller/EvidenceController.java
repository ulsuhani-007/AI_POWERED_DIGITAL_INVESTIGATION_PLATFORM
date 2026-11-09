package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.EvidenceDTO;
import com.cybertrace.m1.service.EvidenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cases/{caseId}/evidence")
@CrossOrigin(origins = "*")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping
    public ResponseEntity<List<EvidenceDTO>> getEvidenceByCase(@PathVariable("caseId") String caseId) {
        List<EvidenceDTO> evidenceList = evidenceService.getEvidenceByCase(caseId);
        return ResponseEntity.ok(evidenceList);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceDTO> uploadEvidence(
            @PathVariable("caseId") String caseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        EvidenceDTO uploaded = evidenceService.uploadEvidence(caseId, file, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    @DeleteMapping("/{evidenceId}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable("caseId") String caseId,
                                               @PathVariable("evidenceId") String evidenceId) {
        evidenceService.deleteEvidence(caseId, evidenceId);
        return ResponseEntity.noContent().build();
    }
}

