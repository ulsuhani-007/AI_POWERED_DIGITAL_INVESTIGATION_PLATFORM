package com.cybertrace.m2.parser;

import com.cybertrace.m2.exception.InvalidEvidenceFileException;
import com.cybertrace.m2.model.EvidenceFileType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy registry for acquiring the appropriate EvidenceParser.
 */
@Component
public class EvidenceParserFactory {

    private final Map<EvidenceFileType, EvidenceParser> parserMap = new EnumMap<>(EvidenceFileType.class);
    private final EvidenceFileTypeDetector detector;

    public EvidenceParserFactory(List<EvidenceParser> parsers, EvidenceFileTypeDetector detector) {
        this.detector = detector;
        for (EvidenceParser parser : parsers) {
            parserMap.put(parser.getSupportedFileType(), parser);
        }
    }

    /**
     * Resolves the parser for a given file type, with auto-detection if AUTO_DETECT is specified.
     *
     * @param fileType          requested EvidenceFileType (can be AUTO_DETECT)
     * @param normalizedHeaders extracted headers from the evidence file
     * @return matching EvidenceParser
     */
    public EvidenceParser resolveParser(EvidenceFileType fileType, Set<String> normalizedHeaders) {
        if (fileType == null || fileType == EvidenceFileType.AUTO_DETECT) {
            EvidenceFileType detectedType = detector.detectType(normalizedHeaders);
            EvidenceParser parser = parserMap.get(detectedType);
            if (parser == null) {
                throw new InvalidEvidenceFileException("No parser registered for detected type: " + detectedType);
            }
            return parser;
        }

        EvidenceParser parser = parserMap.get(fileType);
        if (parser == null) {
            throw new InvalidEvidenceFileException("No parser registered for file type: " + fileType);
        }
        return parser;
    }

    public EvidenceParser getParser(EvidenceFileType fileType) {
        return parserMap.get(fileType);
    }
}
