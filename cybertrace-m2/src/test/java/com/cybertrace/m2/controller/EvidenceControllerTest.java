package com.cybertrace.m2.controller;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EvidenceProcessingResponse;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.TimelineMergeResponse;
import com.cybertrace.m2.service.EvidenceProcessingService;
import com.cybertrace.m2.service.TimelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvidenceController.class)
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvidenceProcessingService processingService;

    @MockBean
    private TimelineService timelineService;

    @Test
    @DisplayName("POST /api/v1/evidence/upload-and-process - successful multipart upload")
    void testUploadAndProcessSuccess() throws Exception {
        String csvContent = "timestamp,user,ipAddress,device,status\n2026-10-26T10:30:00,user1,192.168.1.10,device1,SUCCESS\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "login.csv",
                "text/csv",
                csvContent.getBytes()
        );

        NormalizedEventDTO event = NormalizedEventDTO.builder()
                .eventId("urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479")
                .caseId("case-123")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .eventType(EventType.LOGIN)
                .user("user1")
                .device("device1")
                .ipAddress("192.168.1.10")
                .sourceEvidenceId("evidence-123")
                .description("User user1 login attempt: SUCCESS")
                .build();

        EvidenceProcessingResponse response = EvidenceProcessingResponse.builder()
                .caseId("case-123")
                .sourceEvidenceId("evidence-123")
                .totalProcessedRows(1)
                .validEventsCount(1)
                .errorCount(0)
                .events(List.of(event))
                .errors(List.of())
                .build();

        when(processingService.processEvidence(eq("case-123"), eq("evidence-123"), eq(EvidenceFileType.LOGIN), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(file)
                        .param("caseId", "case-123")
                        .param("sourceEvidenceId", "evidence-123")
                        .param("fileType", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.case_id").value("case-123"))
                .andExpect(jsonPath("$.source_evidence_id").value("evidence-123"))
                .andExpect(jsonPath("$.valid_events_count").value(1))
                .andExpect(jsonPath("$.events[0].event_id").value("urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479"))
                .andExpect(jsonPath("$.events[0].event_type").value("LOGIN"))
                .andExpect(jsonPath("$.events[0].user").value("user1"))
                .andExpect(jsonPath("$.events[0].destination_ip").isEmpty()) // explicit null
                .andExpect(jsonPath("$.events[0].file_name").isEmpty())     // explicit null
                .andExpect(jsonPath("$.events[0].url").isEmpty());           // explicit null
    }

    @Test
    @DisplayName("POST /api/v1/evidence/upload-and-process - missing caseId returns 400")
    void testUploadMissingCaseId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "login.csv",
                "text/csv",
                "test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(file)
                        .param("sourceEvidenceId", "evidence-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/evidence/timeline/merge - successful merge")
    void testTimelineMerge() throws Exception {
        NormalizedEventDTO event = NormalizedEventDTO.builder()
                .eventId("urn:uuid:f47ac10b-58cc-4372-a567-0e02b2c3d479")
                .caseId("case-123")
                .timestamp(LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .eventType(EventType.LOGIN)
                .user("user1")
                .sourceEvidenceId("evidence-123")
                .build();

        TimelineMergeResponse response = TimelineMergeResponse.builder()
                .totalEvents(1)
                .startTime(LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .endTime(LocalDateTime.of(2026, 10, 26, 10, 30, 0))
                .caseIds(List.of("case-123"))
                .sourceEvidenceIds(List.of("evidence-123"))
                .events(List.of(event))
                .build();

        when(timelineService.mergeTimeline(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/evidence/timeline/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(event))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_events").value(1))
                .andExpect(jsonPath("$.start_time").value("2026-10-26T10:30:00"))
                .andExpect(jsonPath("$.case_ids[0]").value("case-123"))
                .andExpect(jsonPath("$.events[0].event_type").value("LOGIN"));
    }
}
