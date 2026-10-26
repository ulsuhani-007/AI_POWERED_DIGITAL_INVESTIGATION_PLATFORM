package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Ingestion parser for network_events.csv files.
 * Expected columns: timestamp, sourceIp, destinationIp, port, bytes, device
 */
@Component
public class NetworkEventsEvidenceParser extends AbstractEvidenceParser {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "timestamp",
            "sourceip",
            "destinationip",
            "port",
            "bytes",
            "device"
    );

    @Override
    public EvidenceFileType getSupportedFileType() {
        return EvidenceFileType.NETWORK;
    }

    @Override
    public EventType getSupportedEventType() {
        return EventType.NETWORK_EVENT;
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
        String sourceIp = getRequiredString(record, "sourceIp");
        String destinationIp = getRequiredString(record, "destinationIp");
        String rawPort = getRequiredString(record, "port");
        String rawBytes = getRequiredString(record, "bytes");
        String device = getRequiredString(record, "device");

        int port;
        try {
            port = Integer.parseInt(rawPort.trim());
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("Port out of range [0-65535]: " + rawPort);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + rawPort);
        }

        long bytes;
        try {
            bytes = Long.parseLong(rawBytes.trim());
            if (bytes < 0) {
                throw new IllegalArgumentException("Bytes cannot be negative: " + rawBytes);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bytes value: " + rawBytes);
        }

        String description = "Network traffic from " + sourceIp + " to " + destinationIp + ":" + port + " (" + bytes + " bytes)";

        return NormalizedEventDTO.builder()
                .eventId(NormalizedEventDTO.generateEventId())
                .caseId(caseId)
                .timestamp(timestamp)
                .eventType(EventType.NETWORK_EVENT)
                .user(null)
                .device(device)
                .ipAddress(sourceIp)
                .destinationIp(destinationIp)
                .fileName(null)
                .action(null)
                .url(null)
                .port(port)
                .bytes(bytes)
                .sourceEvidenceId(sourceEvidenceId)
                .description(description)
                .build();
    }
}
