package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Ingestion parser for login.csv files.
 * Expected columns: timestamp, user, ipAddress, device, status
 */
@Component
public class LoginEvidenceParser extends AbstractEvidenceParser {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "timestamp",
            "user",
            "ipaddress",
            "device",
            "status"
    );

    @Override
    public EvidenceFileType getSupportedFileType() {
        return EvidenceFileType.LOGIN;
    }

    @Override
    public EventType getSupportedEventType() {
        return EventType.LOGIN;
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
        String ipAddress = getRequiredString(record, "ipAddress");
        String device = getRequiredString(record, "device");
        String status = getRequiredString(record, "status");

        String description = "User " + user + " login attempt: " + status;

        return NormalizedEventDTO.builder()
                .eventId(NormalizedEventDTO.generateEventId())
                .caseId(caseId)
                .timestamp(timestamp)
                .eventType(EventType.LOGIN)
                .user(user)
                .device(device)
                .ipAddress(ipAddress)
                .destinationIp(null)
                .fileName(null)
                .action(null)
                .url(null)
                .port(null)
                .bytes(null)
                .sourceEvidenceId(sourceEvidenceId)
                .description(description)
                .build();
    }
}
