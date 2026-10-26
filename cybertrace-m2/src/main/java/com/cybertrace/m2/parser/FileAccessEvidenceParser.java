package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Ingestion parser for file_access.csv files.
 * Expected columns: timestamp, user, fileName, action, device
 */
@Component
public class FileAccessEvidenceParser extends AbstractEvidenceParser {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "timestamp",
            "user",
            "filename",
            "action",
            "device"
    );

    @Override
    public EvidenceFileType getSupportedFileType() {
        return EvidenceFileType.FILE_ACCESS;
    }

    @Override
    public EventType getSupportedEventType() {
        return EventType.FILE_ACCESS;
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
        String fileName = getRequiredString(record, "fileName");
        String action = getRequiredString(record, "action");
        String device = getRequiredString(record, "device");

        String description = "User " + user + " executed action " + action + " on file " + fileName;

        return NormalizedEventDTO.builder()
                .eventId(NormalizedEventDTO.generateEventId())
                .caseId(caseId)
                .timestamp(timestamp)
                .eventType(EventType.FILE_ACCESS)
                .user(user)
                .device(device)
                .ipAddress(null)
                .destinationIp(null)
                .fileName(fileName)
                .action(action)
                .url(null)
                .port(null)
                .bytes(null)
                .sourceEvidenceId(sourceEvidenceId)
                .description(description)
                .build();
    }
}
