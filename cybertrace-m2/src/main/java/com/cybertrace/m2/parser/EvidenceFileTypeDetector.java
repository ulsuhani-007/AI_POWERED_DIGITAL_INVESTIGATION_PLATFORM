package com.cybertrace.m2.parser;

import com.cybertrace.m2.exception.InvalidEvidenceFileException;
import com.cybertrace.m2.model.EvidenceFileType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects evidence file type dynamically by inspecting CSV header signatures.
 */
@Component
public class EvidenceFileTypeDetector {

    private final List<EvidenceParser> parsers;

    public EvidenceFileTypeDetector(List<EvidenceParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Extracts normalized headers from the first row of CSV data.
     *
     * @param headerLine raw first line of CSV
     * @return set of lowercase, trimmed header names
     */
    public Set<String> extractHeadersFromLine(String headerLine) {
        if (headerLine == null || headerLine.trim().isEmpty()) {
            throw new InvalidEvidenceFileException("Evidence file is empty or missing header line");
        }
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setTrim(true)
                    .setIgnoreHeaderCase(true)
                    .build();
            try (CSVParser parser = format.parse(new StringReader(headerLine + "\n"))) {
                List<String> names = parser.getHeaderNames();
                if (names == null || names.isEmpty()) {
                    throw new InvalidEvidenceFileException("Could not extract CSV headers from header line");
                }
                return names.stream()
                        .map(h -> h == null ? "" : h.trim().toLowerCase())
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            throw new InvalidEvidenceFileException("Error reading header line: " + e.getMessage(), e);
        }
    }

    /**
     * Identifies the EvidenceFileType corresponding to the extracted headers.
     *
     * @param normalizedHeaders lowercase, trimmed headers
     * @return detected EvidenceFileType
     */
    public EvidenceFileType detectType(Set<String> normalizedHeaders) {
        if (normalizedHeaders == null || normalizedHeaders.isEmpty()) {
            throw new InvalidEvidenceFileException("Cannot detect file type: headers are empty");
        }

        for (EvidenceParser parser : parsers) {
            if (parser.canParse(normalizedHeaders)) {
                return parser.getSupportedFileType();
            }
        }

        throw new InvalidEvidenceFileException(
                "Unable to auto-detect evidence file type. Headers: " + normalizedHeaders +
                        " do not match any known schema (LOGIN, BROWSER, FILE_ACCESS, NETWORK)"
        );
    }

    /**
     * Helper to read the header line from a buffered reader without consuming remaining content.
     */
    public String readFirstLine(BufferedReader reader) throws IOException {
        reader.mark(8192);
        String firstLine = reader.readLine();
        reader.reset();
        return firstLine;
    }
}
