package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Ingestion parser for browser_activity.csv files.
 * Expected columns: timestamp, user, url, device
 */
@Component
public class BrowserActivityEvidenceParser extends AbstractEvidenceParser {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "timestamp",
            "user",
            "url",
            "device"
    );

    @Override
    public EvidenceFileType getSupportedFileType() {
        return EvidenceFileType.BROWSER;
    }

    @Override
    public EventType getSupportedEventType() {
        return EventType.BROWSER_ACTIVITY;
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_COLUMNS;
    }

    @Override
    protected NormalizedEventDTO parseRecord(
            CSVRecord record,
            long lineNumber,
            String caseId,
            String sourceEvidenceId
    ) {
        LocalDateTime timestamp = parseTimestamp(record, "timestamp");
        String user = getRequiredString(record, "user");
        String url = getRequiredString(record, "url");
        String device = getRequiredString(record, "device");

        String description = "User " + user + " visited URL: " + url;

        return NormalizedEventDTO.builder()
                .eventId(NormalizedEventDTO.generateEventId())
                .caseId(caseId)
                .timestamp(timestamp)
                .eventType(EventType.BROWSER_ACTIVITY)
                .user(user)
                .device(device)
                .ipAddress(null)
                .destinationIp(null)
                .fileName(null)
                .action(null)
                .url(url)
                .port(null)
                .bytes(null)
                .sourceEvidenceId(sourceEvidenceId)
                .description(description)
                .build();
    }
}
