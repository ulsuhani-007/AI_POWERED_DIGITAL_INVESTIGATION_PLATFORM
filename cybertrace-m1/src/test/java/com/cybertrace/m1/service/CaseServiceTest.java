package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.UpdateCaseRequest;
import com.cybertrace.m1.exception.DuplicateResourceException;
import com.cybertrace.m1.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CaseServiceTest {

    @Autowired
    private CaseService caseService;

    @Test
    void testCreateAndGetCase() {
        CreateCaseRequest req = new CreateCaseRequest(
                "CASE-TEST-001",
                "Unauthorized Database Exfiltration",
                "Investigating suspicious data transfer",
                "HIGH",
                "Special Agent Fox"
        );

        CaseDTO created = caseService.createCase(req);

        assertNotNull(created.getId());
        assertEquals("CASE-TEST-001", created.getCaseNumber());
        assertEquals("Unauthorized Database Exfiltration", created.getTitle());
        assertEquals("Unauthorized Database Exfiltration", created.getName());
        assertEquals("HIGH", created.getPriority());
        assertEquals("OPEN", created.getStatus());
        assertEquals("Special Agent Fox", created.getLeadInvestigator());
        assertNotNull(created.getCreatedAt());

        CaseDTO fetched = caseService.getCaseById(created.getId());
        assertEquals(created.getId(), fetched.getId());
        assertEquals("CASE-TEST-001", fetched.getCaseNumber());
    }

    @Test
    void testCreateCaseAutoGeneratesNumber() {
        CreateCaseRequest req = new CreateCaseRequest(
                null,
                "Case with Auto Generated Number",
                "Auto number generation test",
                "LOW",
                "Agent Dana"
        );

        CaseDTO created = caseService.createCase(req);
        assertNotNull(created.getCaseNumber());
        assertTrue(created.getCaseNumber().startsWith("CASE-"));
    }

    @Test
    void testCreateDuplicateCaseNumberThrowsException() {
        CreateCaseRequest req1 = new CreateCaseRequest("CASE-DUP-01", "Case 1", "Desc", "LOW", "Inv");
        caseService.createCase(req1);

        CreateCaseRequest req2 = new CreateCaseRequest("CASE-DUP-01", "Case 2", "Desc", "HIGH", "Inv");
        assertThrows(DuplicateResourceException.class, () -> caseService.createCase(req2));
    }

    @Test
    void testUpdateCase() {
        CreateCaseRequest createReq = new CreateCaseRequest("CASE-UPD-01", "Initial Title", "Initial Desc", "MEDIUM", "Inv");
        CaseDTO created = caseService.createCase(createReq);

        UpdateCaseRequest updateReq = new UpdateCaseRequest();
        updateReq.setTitle("Updated Title");
        updateReq.setDescription("Updated Desc");
        updateReq.setStatus("CLOSED");
        updateReq.setPriority("CRITICAL");

        CaseDTO updated = caseService.updateCase(created.getId(), updateReq);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals("CLOSED", updated.getStatus());
        assertEquals("CRITICAL", updated.getPriority());
    }

    @Test
    void testGetNonExistentCaseThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> caseService.getCaseById("non-existent-uuid"));
    }

    @Test
    void testDeleteCase() {
        CreateCaseRequest req = new CreateCaseRequest("CASE-DEL-01", "To Delete", "Desc", "LOW", "Inv");
        CaseDTO created = caseService.createCase(req);

        caseService.deleteCase(created.getId());

        assertThrows(ResourceNotFoundException.class, () -> caseService.getCaseById(created.getId()));
    }

    @Test
    void testGetAllCases() {
        caseService.createCase(new CreateCaseRequest("CASE-LIST-01", "List Case 1", "D", "LOW", "A1"));
        caseService.createCase(new CreateCaseRequest("CASE-LIST-02", "List Case 2", "D", "HIGH", "A2"));

        List<CaseDTO> all = caseService.getAllCases();
        assertTrue(all.size() >= 2);
    }
}

