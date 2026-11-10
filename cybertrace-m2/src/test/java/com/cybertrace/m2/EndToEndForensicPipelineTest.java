package com.cybertrace.m2;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EvidenceProcessingResponse;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.parser.*;
import com.cybertrace.m2.service.EvidenceProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndToEndForensicPipelineTest {

    private EvidenceProcessingService processingService;

    @BeforeEach
    void setUp() {
        List<EvidenceParser> parsers = List.of(
                new LoginEvidenceParser(),
                new BrowserActivityEvidenceParser(),
                new FileAccessEvidenceParser(),
                new NetworkEventsEvidenceParser(),
                new UnifiedEvidenceParser()
        );
        EvidenceFileTypeDetector detector = new EvidenceFileTypeDetector(parsers);
        EvidenceParserFactory factory = new EvidenceParserFactory(parsers, detector);
        processingService = new EvidenceProcessingService(factory, detector);
    }

    @Test
    @DisplayName("Should successfully ingest and normalize all four real investigation CSV files")
    void testIngestAllFourForensicEvidenceFiles() throws IOException {
        String baseDir = "../test-evidence/";
        File f1 = new File(baseDir + "01_login_events.csv");
        if (!f1.exists()) {
            f1 = new File("test-evidence/01_login_events.csv");
        }
        File f2 = new File(f1.getParentFile(), "02_browser_activity.csv");
        File f3 = new File(f1.getParentFile(), "03_system_log.csv");
        File f4 = new File(f1.getParentFile(), "04_network_events.csv");

        assertThat(f1).exists();
        assertThat(f2).exists();
        assertThat(f3).exists();
        assertThat(f4).exists();

        // 1. Ingest Login Evidence
        String content1 = Files.readString(f1.toPath());
        EvidenceProcessingResponse resp1 = processingService.processEvidenceContent(
                "case-test-001", "ev-login-01", EvidenceFileType.AUTO_DETECT, content1);
        assertThat(resp1.getErrorCount()).isEqualTo(0);
        assertThat(resp1.getValidEventsCount()).isEqualTo(7);

        // 2. Ingest Browser Activity
        String content2 = Files.readString(f2.toPath());
        EvidenceProcessingResponse resp2 = processingService.processEvidenceContent(
                "case-test-001", "ev-browser-02", EvidenceFileType.AUTO_DETECT, content2);
        assertThat(resp2.getErrorCount()).isEqualTo(0);
        assertThat(resp2.getValidEventsCount()).isEqualTo(3);

        // 3. Ingest System Log / File Access
        String content3 = Files.readString(f3.toPath());
        EvidenceProcessingResponse resp3 = processingService.processEvidenceContent(
                "case-test-001", "ev-syslog-03", EvidenceFileType.AUTO_DETECT, content3);
        assertThat(resp3.getErrorCount()).isEqualTo(0);
        assertThat(resp3.getValidEventsCount()).isEqualTo(4);

        // 4. Ingest Network Events
        String content4 = Files.readString(f4.toPath());
        EvidenceProcessingResponse resp4 = processingService.processEvidenceContent(
                "case-test-001", "ev-net-04", EvidenceFileType.AUTO_DETECT, content4);
        assertThat(resp4.getErrorCount()).isEqualTo(0);
        assertThat(resp4.getValidEventsCount()).isEqualTo(4);

        long totalEvents = resp1.getValidEventsCount() + resp2.getValidEventsCount()
                + resp3.getValidEventsCount() + resp4.getValidEventsCount();
        assertThat(totalEvents).isEqualTo(18);

        // Verify key properties
        NormalizedEventDTO largeTransfer = resp4.getEvents().stream()
                .filter(e -> e.getBytes() != null && e.getBytes() > 50_000_000L)
                .findFirst().orElse(null);
        assertThat(largeTransfer).isNotNull();
        assertThat(largeTransfer.getEventType()).isEqualTo(EventType.NETWORK_EVENT);
        assertThat(largeTransfer.getBytes()).isEqualTo(95_000_000L);
    }
}
