package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.service.CaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@WithMockUser(roles = "INVESTIGATOR")
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseService caseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUploadEvidenceAndList() throws Exception {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-CTRL-EV", "Evidence Test Case", "Desc", "MEDIUM", "Inv"));

        String csvData = "timestamp,sourceIp,destinationIp,port,bytes,device\n2026-10-26T10:30:15,192.168.1.10,198.51.100.42,443,15420,device1\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "network_events.csv",
                "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/cases/" + aCase.getId() + "/evidence")
                        .file(file)
                        .param("description", "Captured egress network telemetry"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.caseId").value(aCase.getId()))
                .andExpect(jsonPath("$.fileName").value("network_events.csv"))
                .andExpect(jsonPath("$.fileType").value("NETWORK_CAPTURE"))
                .andExpect(jsonPath("$.hashSha256").isNotEmpty())
                // In standalone tests when M2 is offline, evidence is recorded and status is FAILED; if M2 is online it is PROCESSED
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is("FAILED"), org.hamcrest.Matchers.is("PROCESSED"))));

        // Verify retrieval via GET /api/cases/{caseId}/evidence
        mockMvc.perform(get("/api/cases/" + aCase.getId() + "/evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fileName").value("network_events.csv"));
    }

    @Test
    void testUploadEvidenceForNonExistentCaseReturns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "auth.csv",
                "text/csv",
                "test data".getBytes()
        );

        mockMvc.perform(multipart("/api/cases/missing-case-id/evidence")
                        .file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

