package com.cybertrace.m1.seed;

import com.cybertrace.m1.entity.CaseEntity;
import com.cybertrace.m1.entity.EvidenceEntity;
import com.cybertrace.m1.entity.NormalizedEventEntity;
import com.cybertrace.m1.repository.AnalysisReportRepository;
import com.cybertrace.m1.repository.CaseRepository;
import com.cybertrace.m1.repository.EvidenceRepository;
import com.cybertrace.m1.repository.NormalizedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class HistoricalCasesDataInitializerTest {

    @Autowired
    private HistoricalCasesDataInitializer initializer;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private NormalizedEventRepository normalizedEventRepository;

    @Autowired
    private AnalysisReportRepository analysisReportRepository;

    private static final String[] EXPECTED_CASE_NUMBERS = {
            "CASE-HIST-1978-LUFTHANSA",
            "CASE-HIST-1957-MUNDHRA",
            "CASE-HIST-1984-BHOPAL",
            "CASE-HIST-1992-HARSHAD-MEHTA",
            "CASE-HIST-1932-LINDBERGH",
            "CASE-HIST-1911-MONALISA"
    };

    @Test
    @DisplayName("Verify all 6 historical demonstration cases are seeded with evidence, events, and reports")
    void testHistoricalCasesSeededProperly() {
        for (String caseNumber : EXPECTED_CASE_NUMBERS) {
            Optional<CaseEntity> optCase = caseRepository.findByCaseNumber(caseNumber);
            assertTrue(optCase.isPresent(), "Case should exist: " + caseNumber);

            CaseEntity caseEntity = optCase.get();
            assertNotNull(caseEntity.getId(), "Case ID must not be null");
            assertNotNull(caseEntity.getTitle(), "Title must not be null");
            assertTrue(caseEntity.getDescription().contains("Historical reference demonstration record"),
                    "Description must contain historical reference disclaimer");

            // Verify evidence count
            List<EvidenceEntity> evidenceList = evidenceRepository.findByCaseEntity_IdOrderByUploadedAtDesc(caseEntity.getId());
            assertTrue(evidenceList.size() >= 2, "Case " + caseNumber + " should have at least 2 evidence records, got: " + evidenceList.size());

            for (EvidenceEntity ev : evidenceList) {
                assertNotNull(ev.getHashSha256(), "Evidence hash must not be null");
                assertEquals(64, ev.getHashSha256().length(), "SHA-256 must be 64 characters");
                assertTrue(ev.getEventCount() > 0, "Evidence event count should be > 0");
            }

            // Verify normalized events
            List<NormalizedEventEntity> events = normalizedEventRepository.findByCaseEntity_IdOrderByTimestampAsc(caseEntity.getId());
            assertTrue(events.size() >= 5, "Case " + caseNumber + " should have at least 5 events, got: " + events.size());

            for (NormalizedEventEntity evt : events) {
                assertNotNull(evt.getEventId(), "Event ID must not be null");
                assertTrue(evt.getEventId().startsWith("urn:uuid:"), "Event ID should start with urn:uuid:");
                assertNotNull(evt.getTimestamp(), "Timestamp must not be null");
                assertNotNull(evt.getEventType(), "Event type must not be null");
                assertTrue(evt.getDescription().contains("Historical reference demonstration record"),
                        "Event description must clearly identify demonstration/reference status");
            }

            // Verify analysis report
            assertTrue(analysisReportRepository.findByCaseEntity_Id(caseEntity.getId()).isPresent(),
                    "Analysis report should be initialized for case: " + caseNumber);
        }
    }

    @Test
    @DisplayName("Verify idempotency: re-running initializer does not create duplicate cases, evidence, or events")
    void testInitializerIsIdempotent() {
        long initialCaseCount = caseRepository.count();
        long initialEvidenceCount = evidenceRepository.count();
        long initialEventCount = normalizedEventRepository.count();
        long initialReportCount = analysisReportRepository.count();

        // Run initializer second and third time
        initializer.run();
        initializer.run();

        assertEquals(initialCaseCount, caseRepository.count(), "Case count must remain unchanged after multiple runs");
        assertEquals(initialEvidenceCount, evidenceRepository.count(), "Evidence count must remain unchanged after multiple runs");
        assertEquals(initialEventCount, normalizedEventRepository.count(), "Event count must remain unchanged after multiple runs");
        assertEquals(initialReportCount, analysisReportRepository.count(), "Report count must remain unchanged after multiple runs");
    }
}

