package com.cybertrace.m2.parser;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.NormalizedEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FileAccessEvidenceParserTest {

    private FileAccessEvidenceParser parser;

    @BeforeEach
    void setUp() {
        parser = new FileAccessEvidenceParser();
    }

    @Test
    @DisplayName("Should successfully parse valid file_access.csv entries")
    void testParseValidFileAccessCsv() {
        String csv = """
                timestamp,user,fileName,action,device
                2026-10-26T10:32:30,user1,C:\\data\\secret.docx,READ,device1
                2026-10-26 10:33:45,admin,D:\\backup\\dump.tar,DELETE,server-dc
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-02", "ev-02");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getEvents()).hasSize(2);

        NormalizedEventDTO event1 = result.getEvents().get(0);
        assertThat(event1.getEventType()).isEqualTo(EventType.FILE_ACCESS);
        assertThat(event1.getUser()).isEqualTo("user1");
        assertThat(event1.getFileName()).isEqualTo("C:\\data\\secret.docx");
        assertThat(event1.getAction()).isEqualTo("READ");
        assertThat(event1.getDevice()).isEqualTo("device1");
        assertThat(event1.getDescription()).isEqualTo("User user1 executed action READ on file C:\\data\\secret.docx");
        assertThat(event1.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 32, 30));

        // Strict schema contract verification
        assertThat(event1.getIpAddress()).isNull();
        assertThat(event1.getDestinationIp()).isNull();
        assertThat(event1.getUrl()).isNull();
        assertThat(event1.getPort()).isNull();
        assertThat(event1.getBytes()).isNull();
    }

    @Test
    @DisplayName("Should record errors when required columns are empty")
    void testMissingColumns() {
        String csv = """
                timestamp,user,fileName,action,device
                2026-10-26T10:32:30,user1,,READ,device1
                2026-10-26T10:32:30,user1,C:\\file.txt,,device1
                """;

        ParsedBatchResult result = parser.parse(new StringReader(csv), "case-02", "ev-02");

        assertThat(result.getEvents()).isEmpty();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0).getReason()).contains("Missing required column: fileName");
        assertThat(result.getErrors().get(1).getReason()).contains("Missing required column: action");
    }

    @Test
    @DisplayName("Should verify supported types")
    void testSupportedTypes() {
        assertThat(parser.getSupportedFileType()).isEqualTo(EvidenceFileType.FILE_ACCESS);
        assertThat(parser.getSupportedEventType()).isEqualTo(EventType.FILE_ACCESS);
    }
}
