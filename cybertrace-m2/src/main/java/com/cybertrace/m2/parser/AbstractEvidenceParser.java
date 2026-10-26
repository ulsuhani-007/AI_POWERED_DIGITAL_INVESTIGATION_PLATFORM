package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.ProcessingErrorDTO;
import com.cybertrace.m2.util.TimestampParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base template for CSV evidence parsers ensuring robust error isolation and zero silent failures.
 */
public abstract class AbstractEvidenceParser implements EvidenceParser {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public ParsedBatchResult parse(Reader reader, String caseId, String sourceEvidenceId) {
        List<NormalizedEventDTO> events = new ArrayList<>();
        List<ProcessingErrorDTO> errors = new ArrayList<>();
        long processedRowCount = 0;

        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(false)
                    .build();

            try (CSVParser parser = csvFormat.parse(reader)) {
                // Check if headers match required columns
                List<String> headerNames = parser.getHeaderNames();
                Set<String> normalizedHeaders = headerNames.stream()
                        .map(h -> h == null ? "" : h.trim().toLowerCase())
                        .collect(Collectors.toSet());

                for (String reqCol : getRequiredColumns()) {
                    if (!normalizedHeaders.contains(reqCol.toLowerCase())) {
                        errors.add(ProcessingErrorDTO.builder()
                                .lineNumber(1)
                                .rawRowContent(String.join(",", headerNames))
                                .reason("Missing required column in header: " + reqCol)
                                .build());
                    }
                }

                // If critical header columns are completely absent, stop early to avoid cascading false errors
                if (!errors.isEmpty()) {
                    return ParsedBatchResult.builder()
                            .events(events)
                            .errors(errors)
                            .totalProcessedRows(0)
                            .build();
                }

                for (CSVRecord record : parser) {
                    processedRowCount++;
                    long physicalLineNumber = record.getRecordNumber() + 1;
                    String rawRow = formatRawRow(record);

                    // Check for blank row
                    if (isBlankRow(record)) {
                        errors.add(ProcessingErrorDTO.builder()
                                .lineNumber(physicalLineNumber)
                                .rawRowContent(rawRow)
                                .reason("Blank row")
                                .build());
                        continue;
                    }

                    try {
                        NormalizedEventDTO event = parseRecord(record, physicalLineNumber, caseId, sourceEvidenceId);
                        if (event != null) {
                            events.add(event);
                        }
                    } catch (Exception ex) {
                        log.debug("Row {} parsing error: {}", physicalLineNumber, ex.getMessage());
                        errors.add(ProcessingErrorDTO.builder()
                                .lineNumber(physicalLineNumber)
                                .rawRowContent(rawRow)
                                .reason(ex.getMessage() != null ? ex.getMessage() : "Malformed row data")
                                .build());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Fatal CSV parser failure: {}", ex.getMessage(), ex);
            errors.add(ProcessingErrorDTO.builder()
                    .lineNumber(0)
                    .rawRowContent("N/A")
                    .reason("Malformed CSV file structure: " + ex.getMessage())
                    .build());
        }

        return ParsedBatchResult.builder()
                .events(events)
                .errors(errors)
                .totalProcessedRows(processedRowCount)
                .build();
    }

    /**
     * Subclasses parse individual CSV records into normalized event DTOs.
     */
    protected abstract NormalizedEventDTO parseRecord(
            CSVRecord record,
            long lineNumber,
            String caseId,
            String sourceEvidenceId
    );

    protected String getRequiredString(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            throw new IllegalArgumentException("Missing required column: " + column);
        }
        String val = record.get(column);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required column: " + column);
        }
        return val.trim();
    }

    protected String getOptionalString(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            return null;
        }
        String val = record.get(column);
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    protected LocalDateTime parseTimestamp(CSVRecord record, String column) {
        String raw = getRequiredString(record, column);
        try {
            return TimestampParser.parse(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unparseable date: " + raw);
        }
    }

    protected boolean isBlankRow(CSVRecord record) {
        if (record.size() == 0) {
            return true;
        }
        for (int i = 0; i < record.size(); i++) {
            String val = record.get(i);
            if (val != null && !val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String formatRawRow(CSVRecord record) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < record.size(); i++) {
            values.add(record.get(i));
        }
        return String.join(",", values);
    }
}
