package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Universal forensic evidence parser supporting the unified forensic event schema:
 * id, timestamp, source, event_type, user, ip_address, device, success, bytes, description.
 *
 * Preserves strict NormalizedEventDTO contracts without fabricating missing values.
 */
@Component
public class UnifiedEvidenceParser extends AbstractEvidenceParser {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "timestamp",
            "event_type"
    );

    @Override
    public EvidenceFileType getSupportedFileType() {
        return EvidenceFileType.UNIFIED;
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
    public boolean canParse(Set<String> normalizedHeaders) {
        if (normalizedHeaders == null) {
            return false;
        }
        return normalizedHeaders.contains("event_type") && normalizedHeaders.contains("timestamp");
    }

    @Override
    protected NormalizedEventDTO parseRecord(
            CSVRecord record,
            long lineNumber,
            String caseId,
            String sourceEvidenceId
    ) {
        LocalDateTime timestamp = parseTimestamp(record, "timestamp");
        String rawType = getRequiredStringWithAliases(record, "event_type", "eventtype", "type");
        EventType eventType = mapEventType(rawType);

        String id = getOptionalStringWithAliases(record, "id", "event_id", "eventid");
        if (id == null || id.isBlank()) {
            id = NormalizedEventDTO.generateEventId();
        }

        String user = getOptionalStringWithAliases(record, "user", "username", "account");
        String device = getOptionalStringWithAliases(record, "device", "hostname", "workstation", "endpoint");
        String ipAddress = getOptionalStringWithAliases(record, "ip_address", "ipaddress", "source_ip", "sourceip", "ip");
        String destinationIp = getOptionalStringWithAliases(record, "destination_ip", "destinationip", "dest_ip", "destip");
        String fileName = getOptionalStringWithAliases(record, "filename", "file_name", "file");
        String action = getOptionalStringWithAliases(record, "action", "activity");
        String url = getOptionalStringWithAliases(record, "url", "uri");

        String rawPort = getOptionalStringWithAliases(record, "port", "dest_port", "destination_port");
        Integer port = null;
        if (rawPort != null && !rawPort.isBlank()) {
            try {
                port = Integer.parseInt(rawPort.trim());
            } catch (NumberFormatException ignored) {
                // Ignore unparseable port without failing the batch
            }
        }

        String rawBytes = getOptionalStringWithAliases(record, "bytes", "byte_count", "size");
        Long bytes = null;
        if (rawBytes != null && !rawBytes.isBlank()) {
            try {
                bytes = Long.parseLong(rawBytes.trim());
            } catch (NumberFormatException ignored) {
                // Ignore unparseable bytes without failing the batch
            }
        }

        boolean isSuccess = true;
        String rawSuccess = getOptionalStringWithAliases(record, "success", "status");
        if (rawSuccess != null) {
            String s = rawSuccess.trim().toLowerCase();
            if (s.equals("false") || s.equals("failure") || s.equals("failed") || s.equals("0") || s.equals("denied")) {
                isSuccess = false;
            }
        }
        if (rawType.toUpperCase().contains("FAIL") || rawType.toUpperCase().contains("DENIED")) {
            isSuccess = false;
        }

        String rawDesc = getOptionalStringWithAliases(record, "description", "details", "message");
        String description;
        if (eventType == EventType.LOGIN) {
            String statusStr = isSuccess ? "SUCCESS" : "FAILED";
            if (rawDesc != null && !rawDesc.isBlank()) {
                if (rawDesc.toLowerCase().contains("login attempt:")) {
                    description = rawDesc;
                } else {
                    description = rawDesc + " - login attempt: " + statusStr;
                }
            } else {
                description = "User " + (user != null ? user : "unknown") + " login attempt: " + statusStr;
            }
        } else {
            description = (rawDesc != null && !rawDesc.isBlank())
                    ? rawDesc
                    : ("User " + (user != null ? user : "unknown") + " " + rawType);
        }

        return NormalizedEventDTO.builder()
                .eventId(id)
                .caseId(caseId)
                .timestamp(timestamp)
                .eventType(eventType)
                .user(user)
                .device(device)
                .ipAddress(ipAddress)
                .destinationIp(destinationIp)
                .fileName(fileName)
                .action(action)
                .url(url)
                .port(port)
                .bytes(bytes)
                .sourceEvidenceId(sourceEvidenceId)
                .description(description)
                .build();
    }

    private EventType mapEventType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return EventType.LOGIN;
        }
        String upper = rawType.trim().toUpperCase();
        if (upper.contains("LOGIN") || upper.contains("AUTH")) {
            return EventType.LOGIN;
        }
        if (upper.contains("BROWSER") || upper.contains("WEB") || upper.contains("URL") || upper.contains("VISIT")) {
            return EventType.BROWSER_ACTIVITY;
        }
        if (upper.contains("FILE") || upper.contains("DOC") || upper.contains("ACCESS") || upper.contains("SYSTEM")) {
            return EventType.FILE_ACCESS;
        }
        if (upper.contains("NETWORK") || upper.contains("TRANSFER") || upper.contains("TRAFFIC") || upper.contains("PACKET") || upper.contains("FLOW")) {
            return EventType.NETWORK_EVENT;
        }
        return EventType.LOGIN;
    }

    protected String getOptionalStringWithAliases(CSVRecord record, String... aliases) {
        for (String alias : aliases) {
            if (record.isMapped(alias)) {
                String val = record.get(alias);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return null;
    }

    protected String getRequiredStringWithAliases(CSVRecord record, String... aliases) {
        String val = getOptionalStringWithAliases(record, aliases);
        if (val == null) {
            throw new IllegalArgumentException("Missing required column: " + String.join(" / ", aliases));
        }
        return val;
    }
}
