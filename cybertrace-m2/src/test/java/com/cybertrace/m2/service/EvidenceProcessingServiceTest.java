package com.cybertrace.m2.service;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EvidenceProcessingResponse;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.parser.BrowserActivityEvidenceParser;
import com.cybertrace.m2.parser.EvidenceFileTypeDetector;
import com.cybertrace.m2.parser.EvidenceParser;
import com.cybertrace.m2.parser.EvidenceParserFactory;
import com.cybertrace.m2.parser.FileAccessEvidenceParser;
import com.cybertrace.m2.parser.LoginEvidenceParser;
import com.cybertrace.m2.parser.NetworkEventsEvidenceParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceProcessingServiceTest {

    private EvidenceProcessingService processingService;

    @BeforeEach
    void setUp() {
        List<EvidenceParser> parsers = List.of(
                new LoginEvidenceParser(),
                new BrowserActivityEvidenceParser(),
                new FileAccessEvidenceParser(),
                new NetworkEventsEvidenceParser()
        );
        EvidenceFileTypeDetector detector = new EvidenceFileTypeDetector(parsers);
        EvidenceParserFactory factory = new EvidenceParserFactory(parsers, detector);
        processingService = new EvidenceProcessingService(factory, detector);
    }

    @Test
    @DisplayName("Should auto-detect file type and sort events chronologically strictly ascending")
    void testChronologicalSortingAndAutoDetect() {
        // Deliberately unsorted timestamps
        String csv = """
                timestamp,user,ipAddress,device,status
                2026-10-26T12:00:00,user1,10.0.0.1,device1,SUCCESS
                2026-10-26T09:15:00,user2,10.0.0.2,device2,SUCCESS
                2026-10-26T10:30:00,user3,10.0.0.3,device3,FAILED
                """;

        EvidenceProcessingResponse response = processingService.processEvidenceContent(
                "case-99",
                "ev-99",
                EvidenceFileType.AUTO_DETECT,
                csv
        );

        assertThat(response.getCaseId()).isEqualTo("case-99");
        assertThat(response.getSourceEvidenceId()).isEqualTo("ev-99");
        assertThat(response.getValidEventsCount()).isEqualTo(3);
        assertThat(response.getErrorCount()).isEqualTo(0);

        List<NormalizedEventDTO> events = response.getEvents();
        assertThat(events.get(0).getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 9, 15, 0));
        assertThat(events.get(1).getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 30, 0));
        assertThat(events.get(2).getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 26, 12, 0, 0));
    }

    @Test
    @DisplayName("Should detect and deduplicate identical records within the same batch")
    void testDeduplication() {
        String csv = """
                timestamp,user,ipAddress,device,status
                2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS
                2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS
                2026-10-26T10:35:00,user2,192.168.1.20,device2,SUCCESS
                """;

        EvidenceProcessingResponse response = processingService.processEvidenceContent(
                "case-100",
                "ev-100",
                EvidenceFileType.LOGIN,
                csv
        );

        assertThat(response.getTotalProcessedRows()).isEqualTo(3);
        assertThat(response.getValidEventsCount()).isEqualTo(2);
        assertThat(response.getErrorCount()).isEqualTo(1);

        assertThat(response.getErrors().get(0).getReason())
                .contains("Duplicate event skipped within same batch");
    }

    @Test
    @DisplayName("Should isolate corrupted rows and not crash the batch")
    void testCorruptedRowIsolation() {
        String csv = """
                timestamp,user,ipAddress,device,status
                2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS
                26-99-2026 10:00:00,user2,10.0.0.5,device2,FAILED
                
                ,user3,10.0.0.6,device3,SUCCESS
                2026-10-26T10:45:00,user4,192.168.1.50,device4,SUCCESS
                """;

        EvidenceProcessingResponse response = processingService.processEvidenceContent(
                "case-101",
                "ev-101",
                EvidenceFileType.LOGIN,
                csv
        );

        assertThat(response.getValidEventsCount()).isEqualTo(2);
        assertThat(response.getErrorCount()).isEqualTo(3);

        // Verify valid events are present
        assertThat(response.getEvents().get(0).getUser()).isEqualTo("user1");
        assertThat(response.getEvents().get(1).getUser()).isEqualTo("user4");

        // Verify error reasons
        assertThat(response.getErrors().get(0).getReason()).contains("Unparseable date: 26-99-2026");
        assertThat(response.getErrors().get(1).getReason()).isEqualTo("Blank row");
        assertThat(response.getErrors().get(2).getReason()).contains("Missing required column: timestamp");
    }
}
