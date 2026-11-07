package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.EvidenceDTO;
import com.cybertrace.m1.exception.InvalidInputException;
import com.cybertrace.m1.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EvidenceServiceTest {

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private CaseService caseService;

    @Test
    void testUploadEvidenceComputesSha256() {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-EV-01", "Evidence Case", "Desc", "MEDIUM", "Inv"));

        String csvContent = "timestamp,user,ipAddress,device,status\n2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "login.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        EvidenceDTO uploaded = evidenceService.uploadEvidence(aCase.getId(), file, "Primary user login evidence file");

        assertNotNull(uploaded.getId());
        assertEquals(aCase.getId(), uploaded.getCaseId());
        assertEquals("login.csv", uploaded.getFileName());
        assertEquals("LOGIN_LOG", uploaded.getFileType());
        assertEquals((long) csvContent.getBytes(StandardCharsets.UTF_8).length, uploaded.getFileSizeBytes());
        assertNotNull(uploaded.getHashSha256());
        assertEquals(64, uploaded.getHashSha256().length());
        // In standalone tests when M2 is offline, upload gracefully falls back to FAILED without data loss
        assertEquals("FAILED", uploaded.getStatus());
        assertTrue(uploaded.getDescription().contains("Primary user login evidence file"));

        List<EvidenceDTO> evidenceList = evidenceService.getEvidenceByCase(aCase.getId());
        assertEquals(1, evidenceList.size());
        assertEquals(uploaded.getId(), evidenceList.get(0).getId());
    }

    @Test
    void testUploadEmptyFileThrowsException() {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-EV-02", "Empty Evidence Case", "Desc", "LOW", "Inv"));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(InvalidInputException.class, () ->
                evidenceService.uploadEvidence(aCase.getId(), emptyFile, "Empty evidence"));
    }

    @Test
    void testUploadEvidenceNonExistentCaseThrowsNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "test".getBytes());

        assertThrows(ResourceNotFoundException.class, () ->
                evidenceService.uploadEvidence("non-existent-case-id", file, "desc"));
    }

    @Test
    void testDuplicateEvidenceThrowsConflict() {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-DUP-01", "Duplicate Test", "Desc", "LOW", "Inv"));
        String content = "timestamp,event\n2026-11-06T00:00:00,TEST_EVT\n";
        MockMultipartFile file1 = new MockMultipartFile("file", "log1.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile("file", "log2_renamed.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

        // First upload succeeds
        EvidenceDTO first = evidenceService.uploadEvidence(aCase.getId(), file1, "Initial file");
        assertNotNull(first.getId());

        // Second upload with identical bytes/SHA-256 into same case must throw DuplicateResourceException
        assertThrows(com.cybertrace.m1.exception.DuplicateResourceException.class, () ->
                evidenceService.uploadEvidence(aCase.getId(), file2, "Duplicate file"));
    }

    @Test
    void testDangerousExtensionThrowsInvalidInput() {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-EXT-01", "Extension Test", "Desc", "LOW", "Inv"));
        MockMultipartFile exeFile = new MockMultipartFile("file", "payload.exe", "application/octet-stream", "MZ...dummy binary".getBytes());

        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                evidenceService.uploadEvidence(aCase.getId(), exeFile, "Executable file"));
        assertTrue(ex.getMessage().contains("Prohibited file extension"));
    }

    @Test
    void testPathTraversalFilenameThrowsInvalidInput() {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-TRAV-01", "Traversal Test", "Desc", "LOW", "Inv"));
        MockMultipartFile travFile = new MockMultipartFile("file", "../../etc/passwd.csv", "text/csv", "root:x:0:0".getBytes());

        InvalidInputException ex = assertThrows(InvalidInputException.class, () ->
                evidenceService.uploadEvidence(aCase.getId(), travFile, "Traversal attempt"));
        assertTrue(ex.getMessage().contains("path traversal"));
    }

    @Test
    void testUploadedByChainOfCustodyPreserved() {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-CUSTODY-01", "Chain of Custody", "Desc", "HIGH", "Inv"));
        MockMultipartFile file = new MockMultipartFile("file", "evidence_custody.csv", "text/csv", "timestamp,data\n2026-11-06T01:00:00,DATA\n".getBytes());

        EvidenceDTO uploaded = evidenceService.uploadEvidence(aCase.getId(), file, "Custody tracking");
        assertNotNull(uploaded.getUploadedBy());
        assertFalse(uploaded.getUploadedBy().isBlank());
    }
}

