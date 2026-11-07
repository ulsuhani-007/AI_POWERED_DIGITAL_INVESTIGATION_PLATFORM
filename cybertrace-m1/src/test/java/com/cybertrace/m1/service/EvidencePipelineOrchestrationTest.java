package com.cybertrace.m1.service;

import com.cybertrace.m1.client.M2Client;
import com.cybertrace.m1.client.M3Client;
import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.EvidenceDTO;
import com.cybertrace.m1.dto.FindingDTO;
import com.cybertrace.m1.dto.M2EvidenceProcessingResponse;
import com.cybertrace.m1.dto.NormalizedEventDTO;
import com.cybertrace.m1.entity.AnalysisReportEntity;
import com.cybertrace.m1.entity.EvidenceEntity;
import com.cybertrace.m1.exception.InvalidInputException;
import com.cybertrace.m1.repository.AnalysisReportRepository;
import com.cybertrace.m1.repository.EvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EvidencePipelineOrchestrationTest {

    @Autowired
    private CaseService caseService;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private AnalysisReportRepository analysisReportRepository;

    @Autowired
    private Sha256HashService sha256HashService;

    @Autowired
    private EventService eventService;

    @Autowired
    private AnalysisReportService analysisReportService;

    private CaseDTO testCase;

    @BeforeEach
    void setUp() {
        testCase = caseService.createCase(new CreateCaseRequest(
                "CASE-ORCH-001",
                "Orchestration Verification Case",
                "Testing real M1 -> M2 -> M3 pipeline",
                "HIGH",
                "Lead Investigator"
        ));
    }

    private MockMultipartFile createSampleCsv(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("TASK 8 - Test 1: Successful M1 -> M2 processing returns PROCESSED evidence with SHA-256 and event count")
    void test1_SuccessfulM1ToM2Processing() {
        M2Client m2Client = (caseId, evidenceId, file) -> {
            List<NormalizedEventDTO> events = List.of(
                    new NormalizedEventDTO("EVT-001", caseId, LocalDateTime.now(), "NETWORK",
                            "analyst", "workstation-1", "192.168.1.50", "10.0.0.1",
                            null, "OUTBOUND_CONNECT", null, 443, 1024L, evidenceId, "Normal outbound", true),
                    new NormalizedEventDTO("EVT-002", caseId, LocalDateTime.now(), "NETWORK",
                            "analyst", "workstation-1", "192.168.1.50", "10.0.0.2",
                            null, "OUTBOUND_CONNECT", null, 80, 512L, evidenceId, "HTTP request", true)
            );
            return Optional.of(new M2EvidenceProcessingResponse(caseId, evidenceId, 2, 2, 0, events, List.of()));
        };
        M3Client m3Client = (caseId, events) -> Optional.empty();

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file = createSampleCsv("traffic.csv", "sample,network,data\n1,2,3");
        EvidenceDTO result = pipelineService.uploadEvidence(testCase.getId(), file, "Network capture log");

        assertNotNull(result);
        assertEquals("PROCESSED", result.getStatus());
        assertEquals(2, result.getEventCount());
        assertNotNull(result.getHashSha256());
        assertEquals(64, result.getHashSha256().length());
        assertEquals("traffic.csv", result.getFileName());
        assertEquals(testCase.getId(), result.getCaseId());
    }

    @Test
    @DisplayName("TASK 8 - Test 2: M2 unavailable results in FAILED status, but evidence metadata and SHA-256 are preserved")
    void test2_M2UnavailableStatusFailedEvidencePreserved() {
        M2Client m2Client = (caseId, evidenceId, file) -> Optional.empty(); // Simulates connection refused/timeout
        M3Client m3Client = (caseId, events) -> Optional.empty();

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file = createSampleCsv("offline_test.csv", "timestamp,user\n2026-10-26T10:00:00,user1");
        EvidenceDTO result = pipelineService.uploadEvidence(testCase.getId(), file, "Evidence during M2 downtime");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getHashSha256());
        assertEquals(64, result.getHashSha256().length());

        // Verify evidence is preserved in database and queryable
        List<EvidenceEntity> savedList = evidenceRepository.findByCaseEntity_IdOrderByUploadedAtDesc(testCase.getId());
        assertEquals(1, savedList.size());
        assertEquals("FAILED", savedList.get(0).getStatus().name());
        assertEquals("offline_test.csv", savedList.get(0).getFileName());
        assertNotNull(savedList.get(0).getHashSha256());
    }

    @Test
    @DisplayName("TASK 8 - Test 3: M2 normalized events are properly persisted into M1 database")
    void test3_M2ReturnsNormalizedEvents_PersistedInDB() {
        String eventId1 = "EVT-M2-001";
        String eventId2 = "EVT-M2-002";

        M2Client m2Client = (caseId, evidenceId, file) -> {
            List<NormalizedEventDTO> events = List.of(
                    new NormalizedEventDTO(eventId1, caseId, LocalDateTime.of(2026, 10, 26, 14, 0, 0),
                            "AUTH", "admin", "srv-dc-01", "10.0.0.5", null,
                            null, "LOGIN_SUCCESS", null, 0, 0L, evidenceId, "Domain admin login", true),
                    new NormalizedEventDTO(eventId2, caseId, LocalDateTime.of(2026, 10, 26, 14, 5, 0),
                            "AUTH", "admin", "srv-dc-01", "10.0.0.5", null,
                            null, "PRIVILEGE_ELEVATION", null, 0, 0L, evidenceId, "Token elevation", true)
            );
            return Optional.of(new M2EvidenceProcessingResponse(caseId, evidenceId, 2, 2, 0, events, List.of()));
        };
        M3Client m3Client = (caseId, events) -> Optional.empty();

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file = createSampleCsv("auth.csv", "time,user,action\n1,admin,login");
        pipelineService.uploadEvidence(testCase.getId(), file, "Authentication events");

        // Verify events persisted via eventService
        List<NormalizedEventDTO> persistedEvents = eventService.getEventsByCase(testCase.getId());
        assertEquals(2, persistedEvents.size());
        assertTrue(persistedEvents.stream().anyMatch(e -> eventId1.equals(e.getEventId()) && "admin".equals(e.getUser())));
        assertTrue(persistedEvents.stream().anyMatch(e -> eventId2.equals(e.getEventId()) && "PRIVILEGE_ELEVATION".equals(e.getAction())));
    }

    @Test
    @DisplayName("TASK 8 - Test 4: Duplicate event IDs in M2 output or across multiple uploads are safely deduplicated")
    void test4_DuplicateEventIdsInM2OutputSafelyHandled() {
        String duplicateId = "EVT-DUP-SHARED-01";

        // M2 returns 2 events with the exact same ID in one batch
        M2Client m2Client = (caseId, evidenceId, file) -> {
            List<NormalizedEventDTO> events = List.of(
                    new NormalizedEventDTO(duplicateId, caseId, LocalDateTime.now(), "NETWORK",
                            "user1", "pc1", "192.168.1.1", null, null, "CONNECT", null, 80, 100L, evidenceId, "First copy", true),
                    new NormalizedEventDTO(duplicateId, caseId, LocalDateTime.now(), "NETWORK",
                            "user1", "pc1", "192.168.1.1", null, null, "CONNECT", null, 80, 100L, evidenceId, "Duplicate copy", true)
            );
            return Optional.of(new M2EvidenceProcessingResponse(caseId, evidenceId, 2, 2, 0, events, List.of()));
        };
        M3Client m3Client = (caseId, events) -> Optional.empty();

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file1 = createSampleCsv("dup1.csv", "dup data 1");
        EvidenceDTO upload1 = pipelineService.uploadEvidence(testCase.getId(), file1, "Upload 1");
        assertEquals("PROCESSED", upload1.getStatus());

        List<NormalizedEventDTO> events = eventService.getEventsByCase(testCase.getId());
        assertEquals(1, events.size(), "Only 1 event should be persisted despite duplicate ID in batch");

        // Upload a second file containing the same event ID
        MockMultipartFile file2 = createSampleCsv("dup2.csv", "dup data 2");
        EvidenceDTO upload2 = pipelineService.uploadEvidence(testCase.getId(), file2, "Upload 2");
        assertEquals("PROCESSED", upload2.getStatus());

        List<NormalizedEventDTO> eventsAfterSecond = eventService.getEventsByCase(testCase.getId());
        assertEquals(1, eventsAfterSecond.size(), "Duplicate across multiple uploads must be ignored without crashing");
    }

    @Test
    @DisplayName("TASK 8 - Test 5: M1 -> M3 analysis succeeds and AnalysisReport is persisted")
    void test5_M1ToM3AnalysisSucceedsAndReportPersisted() {
        M2Client m2Client = (caseId, evidenceId, file) -> {
            List<NormalizedEventDTO> events = List.of(
                    new NormalizedEventDTO("EVT-ANOM-01", caseId, LocalDateTime.now(), "NETWORK",
                            "attacker", "pc-compromised", "198.51.100.99", "10.0.0.1",
                            null, "DATA_EXFIL", null, 443, 50000000L, evidenceId, "High volume exfiltration", true)
            );
            return Optional.of(new M2EvidenceProcessingResponse(caseId, evidenceId, 1, 1, 0, events, List.of()));
        };

        M3Client m3Client = (caseId, events) -> {
            FindingDTO finding = new FindingDTO();
            finding.setEventId("EVT-ANOM-01");
            finding.setAnomalyType("DATA_EXFILTRATION");
            finding.setSeverity("CRITICAL");
            finding.setReason("50MB outbound transfer exceeds 99th percentile threshold");
            finding.setScore(0.98);
            finding.setDetectionMethod("ISOLATION_FOREST");

            Map<String, Object> stats = new HashMap<>();
            stats.put("total_bytes", 50000000L);

            return Optional.of(new AnalysisDTO(
                    caseId,
                    events.size(),
                    1,
                    List.of(finding),
                    stats,
                    "Automated analysis identified critical data exfiltration anomaly on event EVT-ANOM-01.",
                    Instant.now()
            ));
        };

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file = createSampleCsv("suspicious_exfil.csv", "exfil data");
        pipelineService.uploadEvidence(testCase.getId(), file, "Suspicious activity capture");

        // Verify analysis report was saved in DB
        Optional<AnalysisReportEntity> reportOpt = analysisReportRepository.findByCaseEntity_Id(testCase.getId());
        assertTrue(reportOpt.isPresent(), "Analysis report entity should be persisted in database");

        AnalysisDTO report = analysisReportService.getAnalysisByCase(testCase.getId());
        assertEquals(testCase.getId(), report.getCaseId());
        assertEquals(1, report.getTotalEvents());
        assertEquals(1, report.getTotalAnomalies());
        assertEquals(1, report.getFindings().size());
        assertEquals("DATA_EXFILTRATION", report.getFindings().get(0).getAnomalyType());
        assertEquals("CRITICAL", report.getFindings().get(0).getSeverity());
        assertTrue(report.getSummary().contains("critical data exfiltration anomaly"));
    }

    @Test
    @DisplayName("TASK 8 - Test 6: M3 unavailable does not crash pipeline; events remain saved and evidence is PROCESSED")
    void test6_M3Unavailable_EventsPersistedEvidenceProcessedNoCrash() {
        M2Client m2Client = (caseId, evidenceId, file) -> {
            List<NormalizedEventDTO> events = List.of(
                    new NormalizedEventDTO("EVT-SURVIVE-01", caseId, LocalDateTime.now(), "SYS",
                            "user", "host", "127.0.0.1", null, null, "EXEC", null, 0, 0L, evidenceId, "Process start", true)
            );
            return Optional.of(new M2EvidenceProcessingResponse(caseId, evidenceId, 1, 1, 0, events, List.of()));
        };
        M3Client m3Client = (caseId, events) -> Optional.empty(); // M3 is down

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file = createSampleCsv("m3_down.csv", "data");
        EvidenceDTO uploaded = pipelineService.uploadEvidence(testCase.getId(), file, "Testing M3 tolerance");

        // Evidence is marked PROCESSED because M2 succeeded and events are in DB
        assertEquals("PROCESSED", uploaded.getStatus());
        assertEquals(1, uploaded.getEventCount());

        // Events remain intact in database
        List<NormalizedEventDTO> eventsInDb = eventService.getEventsByCase(testCase.getId());
        assertEquals(1, eventsInDb.size());
        assertEquals("EVT-SURVIVE-01", eventsInDb.get(0).getEventId());

        // Analysis report entity in DB does not exist
        Optional<AnalysisReportEntity> reportEntity = analysisReportRepository.findByCaseEntity_Id(testCase.getId());
        assertTrue(reportEntity.isEmpty());
    }

    @Test
    @DisplayName("TASK 8 - Test 7: Invalid or empty evidence file throws InvalidInputException without creating records")
    void test7_InvalidOrEmptyEvidenceFileThrowsException() {
        M2Client m2Client = (caseId, evidenceId, file) -> Optional.empty();
        M3Client m3Client = (caseId, events) -> Optional.empty();

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(InvalidInputException.class, () ->
                pipelineService.uploadEvidence(testCase.getId(), emptyFile, "Empty file test"));

        assertThrows(InvalidInputException.class, () ->
                pipelineService.uploadEvidence(testCase.getId(), null, "Null file test"));

        // Verify no evidence was saved
        List<EvidenceEntity> evidenceList = evidenceRepository.findByCaseEntity_IdOrderByUploadedAtDesc(testCase.getId());
        assertEquals(0, evidenceList.size());
    }

    @Test
    @DisplayName("TASK 8 - Test 8: Full pipeline end-to-end integration: Upload -> M2 Parse -> DB Store -> M3 Analyze -> Report Store")
    void test8_FullPipelineEndToEnd() {
        String caseId = testCase.getId();

        M2Client m2Client = (cId, evidenceId, file) -> {
            List<NormalizedEventDTO> events = List.of(
                    new NormalizedEventDTO("E2E-EVT-1", cId, LocalDateTime.of(2026, 10, 26, 8, 30, 0),
                            "AUTH", "admin", "dc01", "10.0.0.10", null, null, "LOGIN", null, 0, 0L, evidenceId, "Admin auth", true),
                    new NormalizedEventDTO("E2E-EVT-2", cId, LocalDateTime.of(2026, 10, 26, 8, 32, 0),
                            "NETWORK", "admin", "dc01", "10.0.0.10", "203.0.113.1", null, "EXFIL", null, 443, 85000000L, evidenceId, "High byte transfer", true)
            );
            return Optional.of(new M2EvidenceProcessingResponse(cId, evidenceId, 2, 2, 0, events, List.of()));
        };

        M3Client m3Client = (cId, events) -> {
            FindingDTO finding = new FindingDTO();
            finding.setEventId("E2E-EVT-2");
            finding.setAnomalyType("NETWORK_VOLUME");
            finding.setSeverity("HIGH");
            finding.setReason("Outbound byte volume of 85MB significantly exceeds typical cluster egress");
            finding.setScore(0.95);
            finding.setDetectionMethod("STATISTICAL_THRESHOLD");

            Map<String, Object> stats = new HashMap<>();
            stats.put("total_events", events.size());
            stats.put("flagged_events", 1);

            return Optional.of(new AnalysisDTO(
                    cId,
                    events.size(),
                    1,
                    List.of(finding),
                    stats,
                    "End-to-end forensic analysis completed. 1 critical anomaly detected in egress traffic.",
                    Instant.now()
            ));
        };

        EvidenceService pipelineService = new EvidenceService(
                evidenceRepository, caseService, sha256HashService,
                m2Client, eventService, m3Client, analysisReportService
        );

        MockMultipartFile file = createSampleCsv("investigation_log.csv", "timestamp,user,action,bytes\n2026-10-26,admin,EXFIL,85000000");
        EvidenceDTO uploaded = pipelineService.uploadEvidence(caseId, file, "Primary compromise telemetry");

        // Step 1: Verify Evidence status & integrity
        assertEquals("PROCESSED", uploaded.getStatus());
        assertEquals(2, uploaded.getEventCount());
        assertNotNull(uploaded.getHashSha256());
        assertEquals(64, uploaded.getHashSha256().length());

        // Step 2: Verify normalized events persisted in database
        List<NormalizedEventDTO> savedEvents = eventService.getEventsByCase(caseId);
        assertEquals(2, savedEvents.size());
        assertTrue(savedEvents.stream().anyMatch(e -> "E2E-EVT-1".equals(e.getEventId())));
        assertTrue(savedEvents.stream().anyMatch(e -> "E2E-EVT-2".equals(e.getEventId())));

        // Step 3: Verify M3 analysis report persisted in database
        Optional<AnalysisReportEntity> reportEntityOpt = analysisReportRepository.findByCaseEntity_Id(caseId);
        assertTrue(reportEntityOpt.isPresent());

        AnalysisDTO report = analysisReportService.getAnalysisByCase(caseId);
        assertEquals(2, report.getTotalEvents());
        assertEquals(1, report.getTotalAnomalies());
        assertEquals(1, report.getFindings().size());
        assertEquals("NETWORK_VOLUME", report.getFindings().get(0).getAnomalyType());
        assertEquals("E2E-EVT-2", report.getFindings().get(0).getEventId());
        assertTrue(report.getSummary().contains("End-to-end forensic analysis completed"));
    }
}
