package com.cybertrace.m2.service;

import com.cybertrace.m2.exception.InvalidEvidenceFileException;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EvidenceProcessingResponse;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.ProcessingErrorDTO;
import com.cybertrace.m2.parser.EvidenceFileTypeDetector;
import com.cybertrace.m2.parser.EvidenceParser;
import com.cybertrace.m2.parser.EvidenceParserFactory;
import com.cybertrace.m2.parser.ParsedBatchResult;
import com.cybertrace.m2.util.NormalizedEventComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core engine service for ingesting, validating, parsing, deduplicating,
 * and normalizing evidence CSV files into standardized timeline events.
 */
@Service
public class EvidenceProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceProcessingService.class);

    private final EvidenceParserFactory parserFactory;
    private final EvidenceFileTypeDetector fileTypeDetector;

    @Value("${cybertrace.deduplication.enabled:true}")
    private boolean deduplicationEnabled = true;

    @Value("${cybertrace.deduplication.audit-errors:true}")
    private boolean auditDuplicateErrors = true;

    public EvidenceProcessingService(
            EvidenceParserFactory parserFactory,
            EvidenceFileTypeDetector fileTypeDetector
    ) {
        this.parserFactory = parserFactory;
        this.fileTypeDetector = fileTypeDetector;
    }

    /**
     * Processes an uploaded multipart CSV file.
     *
     * @param caseId           case identifier
     * @param sourceEvidenceId source evidence identifier
     * @param fileType         requested or AUTO_DETECT file type
     * @param file             uploaded multipart file
     * @return EvidenceProcessingResponse with chronological events and errors
     */
    public EvidenceProcessingResponse processEvidence(
            String caseId,
            String sourceEvidenceId,
            EvidenceFileType fileType,
            MultipartFile file
    ) {
        validateInputs(caseId, sourceEvidenceId);
        if (file == null || file.isEmpty()) {
            throw new InvalidEvidenceFileException("Uploaded evidence file is null or empty");
        }

        try {
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            return processEvidenceContent(caseId, sourceEvidenceId, fileType, fileContent);
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", e.getMessage(), e);
            throw new InvalidEvidenceFileException("Failed to read uploaded file content: " + e.getMessage(), e);
        }
    }

    /**
     * Processes raw string CSV content.
     */
    public EvidenceProcessingResponse processEvidenceContent(
            String caseId,
            String sourceEvidenceId,
            EvidenceFileType fileType,
            String fileContent
    ) {
        validateInputs(caseId, sourceEvidenceId);
        if (fileContent == null || fileContent.trim().isEmpty()) {
            throw new InvalidEvidenceFileException("Evidence content is null or empty");
        }

        // Determine header line
        String firstLine;
        try (BufferedReader reader = new BufferedReader(new StringReader(fileContent))) {
            firstLine = reader.readLine();
        } catch (IOException e) {
            throw new InvalidEvidenceFileException("Failed to read header line: " + e.getMessage(), e);
        }

        Set<String> headers = fileTypeDetector.extractHeadersFromLine(firstLine);
        EvidenceParser parser = parserFactory.resolveParser(fileType, headers);

        // Execute parsing
        ParsedBatchResult batchResult = parser.parse(new StringReader(fileContent), caseId, sourceEvidenceId);

        // Deduplicate events
        List<NormalizedEventDTO> deduplicatedEvents = new ArrayList<>();
        List<ProcessingErrorDTO> allErrors = new ArrayList<>(batchResult.getErrors());
        Set<String> seenKeys = new HashSet<>();

        for (NormalizedEventDTO event : batchResult.getEvents()) {
            if (deduplicationEnabled) {
                String dedupeKey = computeDeduplicationKey(event);
                if (!seenKeys.add(dedupeKey)) {
                    if (auditDuplicateErrors) {
                        allErrors.add(ProcessingErrorDTO.builder()
                                .lineNumber(-1)
                                .rawRowContent("Deduplicated: " + dedupeKey)
                                .reason("Duplicate event skipped within same batch: " + event.getEventType() + " at " + event.getTimestamp())
                                .build());
                    }
                    continue;
                }
            }
            deduplicatedEvents.add(event);
        }

        // Sort chronologically ascending by timestamp, then event_type, then event_id
        deduplicatedEvents.sort(NormalizedEventComparator.INSTANCE);

        return EvidenceProcessingResponse.builder()
                .caseId(caseId)
                .sourceEvidenceId(sourceEvidenceId)
                .totalProcessedRows(batchResult.getTotalProcessedRows())
                .validEventsCount(deduplicatedEvents.size())
                .errorCount(allErrors.size())
                .events(deduplicatedEvents)
                .errors(allErrors)
                .build();
    }

    /**
     * Deduplication signature based on: timestamp + user + device + type/action.
     */
    public String computeDeduplicationKey(NormalizedEventDTO event) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                event.getTimestamp() != null ? event.getTimestamp().toString() : "",
                event.getUser() != null ? event.getUser() : "",
                event.getDevice() != null ? event.getDevice() : "",
                event.getEventType() != null ? event.getEventType().name() : "",
                event.getAction() != null ? event.getAction() : "",
                event.getFileName() != null ? event.getFileName() : "",
                event.getUrl() != null ? event.getUrl() : "",
                event.getIpAddress() != null ? event.getIpAddress() : ""
        );
    }

    private void validateInputs(String caseId, String sourceEvidenceId) {
        if (caseId == null || caseId.trim().isEmpty()) {
            throw new IllegalArgumentException("caseId is required and cannot be blank");
        }
        if (sourceEvidenceId == null || sourceEvidenceId.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceEvidenceId is required and cannot be blank");
        }
    }
}
