package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EventType;

import java.io.Reader;
import java.util.Set;

/**
 * Strategy interface for parsing specific evidence CSV formats into normalized events.
 */
public interface EvidenceParser {

    /**
     * @return supported evidence file category
     */
    EvidenceFileType getSupportedFileType();

    /**
     * @return corresponding normalized event type
     */
    EventType getSupportedEventType();

    /**
     * @return set of normalized (lowercase, trimmed) required column names
     */
    Set<String> getRequiredColumns();

    /**
     * Checks if the given header set matches this parser's schema.
     *
     * @param normalizedHeaders set of lowercase, trimmed header names
     * @return true if all required columns are present
     */
    default boolean canParse(Set<String> normalizedHeaders) {
        if (normalizedHeaders == null) {
            return false;
        }
        return normalizedHeaders.containsAll(getRequiredColumns());
    }

    /**
     * Parses the CSV input stream/reader and normalizes rows into events.
     * Corrupted rows are isolated into errors without failing the batch.
     *
     * @param reader           input reader
     * @param caseId           associated case ID
     * @param sourceEvidenceId ID of the evidence artifact
     * @return ParsedBatchResult containing valid events and error audit reports
     */
    ParsedBatchResult parse(Reader reader, String caseId, String sourceEvidenceId);
}
