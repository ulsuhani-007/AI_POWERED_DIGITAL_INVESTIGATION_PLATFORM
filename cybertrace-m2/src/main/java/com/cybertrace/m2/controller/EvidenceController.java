package com.cybertrace.m2.controller;

import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EvidenceProcessingResponse;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.TimelineMergeResponse;
import com.cybertrace.m2.service.EvidenceProcessingService;
import com.cybertrace.m2.service.TimelineService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller exposing endpoints for evidence ingestion, parsing,
 * normalization, and chronological timeline merging.
 */
@RestController
@RequestMapping("/api/v1/evidence")
@Validated
public class EvidenceController {

    private final EvidenceProcessingService processingService;
    private final TimelineService timelineService;

    public EvidenceController(
            EvidenceProcessingService processingService,
            TimelineService timelineService
    ) {
        this.processingService = processingService;
        this.timelineService = timelineService;
    }

    /**
     * Ingests and processes a CSV evidence file into a normalized, chronological event list.
     *
     * @param caseId           case identifier
     * @param sourceEvidenceId source evidence identifier
     * @param fileType         evidence category (LOGIN, BROWSER, FILE_ACCESS, NETWORK, or AUTO_DETECT)
     * @param file             CSV multipart file
     * @return EvidenceProcessingResponse containing valid events and error audit reports
     */
    @PostMapping(value = "/upload-and-process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceProcessingResponse> uploadAndProcess(
            @RequestParam("caseId") @NotBlank(message = "caseId must not be blank") String caseId,
            @RequestParam("sourceEvidenceId") @NotBlank(message = "sourceEvidenceId must not be blank") String sourceEvidenceId,
            @RequestParam(value = "fileType", defaultValue = "AUTO_DETECT") EvidenceFileType fileType,
            @RequestParam("file") @NotNull(message = "file is required") MultipartFile file
    ) {
        EvidenceProcessingResponse response = processingService.processEvidence(
                caseId,
                sourceEvidenceId,
                fileType,
                file
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Merges normalized events from multiple evidence sources into a unified chronological timeline.
     *
     * @param events list of normalized event DTOs
     * @return consolidated timeline response with summary metadata
     */
    @PostMapping(value = "/timeline/merge", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TimelineMergeResponse> mergeTimeline(
            @RequestBody @NotNull(message = "Event list must not be null") List<NormalizedEventDTO> events
    ) {
        TimelineMergeResponse response = timelineService.mergeTimeline(events);
        return ResponseEntity.ok(response);
    }
}
