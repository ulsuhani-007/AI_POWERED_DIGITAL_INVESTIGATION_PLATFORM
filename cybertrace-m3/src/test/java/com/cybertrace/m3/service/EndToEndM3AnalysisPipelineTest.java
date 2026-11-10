package com.cybertrace.m3.service;

import com.cybertrace.m3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndToEndM3AnalysisPipelineTest {

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        BruteForceDetectionService bruteForce = new BruteForceDetectionService(3, java.time.Duration.ofMinutes(15));
        NetworkVolumeDetectionService networkVolume = new NetworkVolumeDetectionService();
        OffHoursDetectionService offHours = new OffHoursDetectionService(9, 18);
        MachineLearningAnomalyDetectionService mlService = new MachineLearningAnomalyDetectionService();
        InvestigationSummaryService summaryService = new InvestigationSummaryService();

        analysisService = new AnalysisService(
                bruteForce,
                networkVolume,
                offHours,
                mlService,
                summaryService
        );
    }

    @Test
    @DisplayName("Should detect security anomalies and produce AI summary for all 18 normalized events")
    void testAnalyzeCumulativeInvestigationEvents() {
        List<NormalizedEventDTO> events = new ArrayList<>();

        // 1. Logins
        events.add(NormalizedEventDTO.builder().eventId("L001").timestamp(LocalDateTime.of(2026, 11, 9, 8, 15, 0)).eventType("LOGIN").user("alice").device("WS-01").ipAddress("10.10.10.21").description("Normal morning login (login attempt: SUCCESS)").build());
        events.add(NormalizedEventDTO.builder().eventId("L002").timestamp(LocalDateTime.of(2026, 11, 9, 8, 17, 0)).eventType("LOGIN").user("bob").device("WS-02").ipAddress("10.10.10.22").description("Normal morning login (login attempt: SUCCESS)").build());
        events.add(NormalizedEventDTO.builder().eventId("L003").timestamp(LocalDateTime.of(2026, 11, 9, 23, 48, 0)).eventType("LOGIN").user("alice").device("WS-01").ipAddress("10.10.10.21").description("Late-night login (login attempt: SUCCESS)").build());
        events.add(NormalizedEventDTO.builder().eventId("L004").timestamp(LocalDateTime.of(2026, 11, 10, 0, 3, 0)).eventType("LOGIN").user("alice").device("WS-01").ipAddress("10.10.10.21").description("Failed login (login attempt: FAILED)").build());
        events.add(NormalizedEventDTO.builder().eventId("L005").timestamp(LocalDateTime.of(2026, 11, 10, 0, 4, 0)).eventType("LOGIN").user("alice").device("WS-01").ipAddress("10.10.10.21").description("Failed login (login attempt: FAILED)").build());
        events.add(NormalizedEventDTO.builder().eventId("L006").timestamp(LocalDateTime.of(2026, 11, 10, 0, 5, 0)).eventType("LOGIN").user("alice").device("WS-01").ipAddress("10.10.10.21").description("Failed login (login attempt: FAILED)").build());
        events.add(NormalizedEventDTO.builder().eventId("L007").timestamp(LocalDateTime.of(2026, 11, 10, 0, 6, 0)).eventType("LOGIN").user("alice").device("WS-01").ipAddress("10.10.10.21").description("Successful login after failures (login attempt: SUCCESS)").build());

        // 2. Browser
        events.add(NormalizedEventDTO.builder().eventId("B001").timestamp(LocalDateTime.of(2026, 11, 9, 9, 10, 0)).eventType("BROWSER_ACTIVITY").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(1200L).description("Visited internal portal").build());
        events.add(NormalizedEventDTO.builder().eventId("B002").timestamp(LocalDateTime.of(2026, 11, 9, 9, 12, 0)).eventType("BROWSER_ACTIVITY").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(1500L).description("Visited company mail").build());
        events.add(NormalizedEventDTO.builder().eventId("B003").timestamp(LocalDateTime.of(2026, 11, 10, 0, 10, 0)).eventType("BROWSER_ACTIVITY").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(900L).description("Visited external file-sharing site").build());

        // 3. File Access
        events.add(NormalizedEventDTO.builder().eventId("S001").timestamp(LocalDateTime.of(2026, 11, 9, 10, 0, 0)).eventType("FILE_ACCESS").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(4500L).description("Opened finance document").build());
        events.add(NormalizedEventDTO.builder().eventId("S002").timestamp(LocalDateTime.of(2026, 11, 9, 10, 5, 0)).eventType("FILE_ACCESS").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(6200L).description("Opened payroll document").build());
        events.add(NormalizedEventDTO.builder().eventId("S003").timestamp(LocalDateTime.of(2026, 11, 10, 0, 12, 0)).eventType("FILE_ACCESS").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(7800L).description("Accessed confidential finance archive").build());
        events.add(NormalizedEventDTO.builder().eventId("S004").timestamp(LocalDateTime.of(2026, 11, 10, 0, 14, 0)).eventType("FILE_ACCESS").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(8100L).description("Accessed confidential payroll archive").build());

        // 4. Network
        events.add(NormalizedEventDTO.builder().eventId("N001").timestamp(LocalDateTime.of(2026, 11, 9, 11, 0, 0)).eventType("NETWORK_EVENT").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(12000L).description("Normal outbound transfer").build());
        events.add(NormalizedEventDTO.builder().eventId("N002").timestamp(LocalDateTime.of(2026, 11, 9, 11, 5, 0)).eventType("NETWORK_EVENT").user("bob").device("WS-02").ipAddress("10.10.10.22").bytes(9000L).description("Normal outbound transfer").build());
        events.add(NormalizedEventDTO.builder().eventId("N003").timestamp(LocalDateTime.of(2026, 11, 10, 0, 20, 0)).eventType("NETWORK_EVENT").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(95000000L).description("Very large outbound transfer").build());
        events.add(NormalizedEventDTO.builder().eventId("N004").timestamp(LocalDateTime.of(2026, 11, 10, 0, 21, 0)).eventType("NETWORK_EVENT").user("alice").device("WS-01").ipAddress("10.10.10.21").bytes(88000000L).description("Large outbound transfer").build());

        AnalysisReportDTO report = analysisService.analyze(events);

        assertThat(report).isNotNull();
        assertThat(report.getTotalEvents()).isEqualTo(18);
        assertThat(report.getTotalAnomalies()).isGreaterThan(0);
        assertThat(report.getFindings()).isNotEmpty();

        System.out.println("Total detected anomalies: " + report.getTotalAnomalies());
        for (AnomalyFindingDTO anomaly : report.getFindings()) {
            System.out.println(" - " + anomaly.getAnomalyType() + " | " + anomaly.getSeverity() + " | " + anomaly.getReason());
        }

        System.out.println("Forensic Summary: " + report.getSummary());
        assertThat(report.getSummary()).isNotBlank();
    }
}
