package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.CaseDTO;
import com.cybertrace.m1.dto.CreateCaseRequest;
import com.cybertrace.m1.dto.NormalizedEventDTO;
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

import java.time.LocalDateTime;
import java.util.List;

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
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseService caseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testIngestAndGetEvents() throws Exception {
        CaseDTO aCase = caseService.createCase(new CreateCaseRequest("CASE-CTRL-EVT", "Events Case", "Desc", "MEDIUM", "Inv"));

        NormalizedEventDTO evt1 = new NormalizedEventDTO(
                "urn:uuid:evt-001",
                aCase.getId(),
                LocalDateTime.of(2026, 10, 26, 10, 30, 0),
                "LOGIN",
                "admin",
                "DESKTOP-01",
                "192.168.1.10",
                null,
                null,
                "LOGIN_SUCCESS",
                null,
                null,
                null,
                "ev-101",
                "Admin successful login",
                true
        );

        NormalizedEventDTO evt2 = new NormalizedEventDTO(
                "urn:uuid:evt-002",
                aCase.getId(),
                LocalDateTime.of(2026, 10, 26, 10, 35, 0),
                "NETWORK_EVENT",
                "admin",
                "DESKTOP-01",
                "192.168.1.10",
                "198.51.100.42",
                null,
                null,
                null,
                443,
                5500000L,
                "ev-101",
                "Outbound data burst",
                true
        );

        List<NormalizedEventDTO> eventList = List.of(evt1, evt2);

        // POST /api/cases/{caseId}/events
        mockMvc.perform(post("/api/cases/" + aCase.getId() + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventList)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].event_id").value("urn:uuid:evt-001"))
                .andExpect(jsonPath("$[0].id").value("urn:uuid:evt-001"))
                .andExpect(jsonPath("$[0].case_id").value(aCase.getId()))
                .andExpect(jsonPath("$[0].event_type").value("LOGIN"))
                .andExpect(jsonPath("$[0].user").value("admin"));

        // GET /api/cases/{caseId}/events
        mockMvc.perform(get("/api/cases/" + aCase.getId() + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].event_id").value("urn:uuid:evt-001"))
                .andExpect(jsonPath("$[1].event_id").value("urn:uuid:evt-002"))
                .andExpect(jsonPath("$[1].bytes").value(5500000));
    }
}

