package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.CreateCaseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@WithMockUser(roles = "INVESTIGATOR")
class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateCaseAndGet() throws Exception {
        CreateCaseRequest req = new CreateCaseRequest(
                "CASE-CTRL-01",
                "Controller Integration Case",
                "Case created via REST endpoint",
                "HIGH",
                "Lead Investigator Holmes"
        );

        String json = objectMapper.writeValueAsString(req);

        String responseBody = mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.caseNumber").value("CASE-CTRL-01"))
                .andExpect(jsonPath("$.title").value("Controller Integration Case"))
                .andExpect(jsonPath("$.name").value("Controller Integration Case"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        String caseId = objectMapper.readTree(responseBody).get("id").asText();

        // Get by ID
        mockMvc.perform(get("/api/cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.caseNumber").value("CASE-CTRL-01"));

        // Get All
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testCreateCaseValidationFailureBlankTitle() throws Exception {
        CreateCaseRequest req = new CreateCaseRequest(
                "CASE-BAD-01",
                "   ",
                "Description",
                "LOW",
                "Investigator"
        );

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void testGetCaseNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/cases/non-existent-id-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testDeleteCaseReturns204() throws Exception {
        CreateCaseRequest req = new CreateCaseRequest(
                "CASE-DEL-CTRL",
                "Delete Case",
                "Desc",
                "LOW",
                "Inv"
        );

        String responseBody = mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String caseId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/cases/" + caseId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cases/" + caseId))
                .andExpect(status().isNotFound());
    }
}

