package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.FindingDTO;
import com.cybertrace.m1.service.CaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@WithMockUser(roles = "INVESTIGATOR")
class AnalysisReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseService caseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetInitialEmptyAnalysisAndSaveReport() throws Exception {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-CTRL-ANALYSIS", "Analysis Test Case", "Desc", "CRITICAL", "Inv"));

        // 1. Initial GET on new case: returns default initial report with 200 OK
        mockMvc.perform(get("/api/cases/" + aCase.getId() + "/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.case_id").value(aCase.getId()))
                .andExpect(jsonPath("$.total_anomalies").value(0))
                .andExpect(jsonPath("$.findings", hasSize(0)))
                .andExpect(jsonPath("$.summary").isNotEmpty());

        // 2. Save Analysis Report
        FindingDTO finding = new FindingDTO(
                "fnd-001",
                "urn:uuid:evt-999",
                LocalDateTime.of(2026, 10, 26, 23, 15, 0),
                "NETWORK_EVENT",
                "ANOMALOUS_EGRESS_BURST",
                "Unusually high network exfiltration (5.5 MB) outside operating hours",
                "CRITICAL",
                0.96,
                "Isolation Forest + MAD Z-Score",
                "192.168.1.10",
                "Isolate endpoint DESKTOP-01 from network"
        );

        Map<String, Object> stats = Map.of(
                "total_events", 120,
                "critical_anomalies", 1,
                "max_anomaly_score", 0.96,
                "primary_threat_vector", "Data Exfiltration"
        );

        AnalysisDTO analysisDTO = new AnalysisDTO(
                aCase.getId(),
                120,
                1,
                List.of(finding),
                stats,
                "Comprehensive forensic analysis revealed critical egress spike.",
                Instant.now()
        );

        mockMvc.perform(post("/api/cases/" + aCase.getId() + "/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analysisDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.case_id").value(aCase.getId()))
                .andExpect(jsonPath("$.total_anomalies").value(1))
                .andExpect(jsonPath("$.findings", hasSize(1)))
                .andExpect(jsonPath("$.findings[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.findings[0].score").value(0.96))
                .andExpect(jsonPath("$.summary").value("Comprehensive forensic analysis revealed critical egress spike."));

        // 3. Verify subsequent GET returns the persisted report
        mockMvc.perform(get("/api/cases/" + aCase.getId() + "/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.case_id").value(aCase.getId()))
                .andExpect(jsonPath("$.total_anomalies").value(1))
                .andExpect(jsonPath("$.findings", hasSize(1)))
                .andExpect(jsonPath("$.findings[0].id").value("fnd-001"))
                .andExpect(jsonPath("$.findings[0].event_id").value("urn:uuid:evt-999"))
                .andExpect(jsonPath("$.findings[0].affected_entity").value("192.168.1.10"))
                .andExpect(jsonPath("$.statistics.primary_threat_vector").value("Data Exfiltration"));
    }

    @Test
    void testGetAnalysisMissingCaseReturns404() throws Exception {
        mockMvc.perform(get("/api/cases/non-existent-case-uuid/analysis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

