package com.cybertrace.m2;

import com.cybertrace.m2.model.EventType;
import com.cybertrace.m2.model.EvidenceFileType;
import com.cybertrace.m2.model.EvidenceProcessingResponse;
import com.cybertrace.m2.model.NormalizedEventDTO;
import com.cybertrace.m2.model.TimelineMergeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CyberTraceM2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End: Ingest 4 CSV evidence files, auto-detect, verify schema, and merge into unified timeline")
    void testEndToEndEvidenceIngestionAndTimelineMerge() throws Exception {
        List<NormalizedEventDTO> allEvents = new ArrayList<>();

        // 1. Ingest login.csv
        byte[] loginBytes = Files.readAllBytes(new ClassPathResource("samples/login.csv").getFile().toPath());
        MockMultipartFile loginFile = new MockMultipartFile("file", "login.csv", "text/csv", loginBytes);

        MvcResult loginResult = mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(loginFile)
                        .param("caseId", "case-alpha")
                        .param("sourceEvidenceId", "ev-login-01")
                        .param("fileType", "AUTO_DETECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.case_id").value("case-alpha"))
                .andExpect(jsonPath("$.source_evidence_id").value("ev-login-01"))
                .andExpect(jsonPath("$.valid_events_count").value(4))
                .andExpect(jsonPath("$.error_count").value(0))
                .andReturn();

        EvidenceProcessingResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                EvidenceProcessingResponse.class
        );
        allEvents.addAll(loginResponse.getEvents());

        // Verify Strict JSON schema contract for LOGIN:
        // non-applicable fields must be explicitly serialized as null
        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        JsonNode firstEvent = loginJson.get("events").get(0);
        assertThat(firstEvent.get("event_type").asText()).isEqualTo("LOGIN");
        assertThat(firstEvent.get("event_id").asText()).startsWith("urn:uuid:");
        assertThat(firstEvent.get("destination_ip").isNull()).isTrue();
        assertThat(firstEvent.get("file_name").isNull()).isTrue();
        assertThat(firstEvent.get("action").isNull()).isTrue();
        assertThat(firstEvent.get("url").isNull()).isTrue();
        assertThat(firstEvent.get("port").isNull()).isTrue();
        assertThat(firstEvent.get("bytes").isNull()).isTrue();

        // 2. Ingest browser_activity.csv
        byte[] browserBytes = Files.readAllBytes(new ClassPathResource("samples/browser_activity.csv").getFile().toPath());
        MockMultipartFile browserFile = new MockMultipartFile("file", "browser_activity.csv", "text/csv", browserBytes);

        MvcResult browserResult = mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(browserFile)
                        .param("caseId", "case-alpha")
                        .param("sourceEvidenceId", "ev-browser-01")
                        .param("fileType", "AUTO_DETECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid_events_count").value(4))
                .andExpect(jsonPath("$.error_count").value(0))
                .andReturn();

        EvidenceProcessingResponse browserResponse = objectMapper.readValue(
                browserResult.getResponse().getContentAsString(),
                EvidenceProcessingResponse.class
        );
        allEvents.addAll(browserResponse.getEvents());

        // 3. Ingest file_access.csv
        byte[] fileBytes = Files.readAllBytes(new ClassPathResource("samples/file_access.csv").getFile().toPath());
        MockMultipartFile fileAccessFile = new MockMultipartFile("file", "file_access.csv", "text/csv", fileBytes);

        MvcResult fileResult = mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(fileAccessFile)
                        .param("caseId", "case-alpha")
                        .param("sourceEvidenceId", "ev-files-01")
                        .param("fileType", "AUTO_DETECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid_events_count").value(4))
                .andExpect(jsonPath("$.error_count").value(0))
                .andReturn();

        EvidenceProcessingResponse fileResponse = objectMapper.readValue(
                fileResult.getResponse().getContentAsString(),
                EvidenceProcessingResponse.class
        );
        allEvents.addAll(fileResponse.getEvents());

        // 4. Ingest network_events.csv
        byte[] networkBytes = Files.readAllBytes(new ClassPathResource("samples/network_events.csv").getFile().toPath());
        MockMultipartFile networkFile = new MockMultipartFile("file", "network_events.csv", "text/csv", networkBytes);

        MvcResult networkResult = mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(networkFile)
                        .param("caseId", "case-alpha")
                        .param("sourceEvidenceId", "ev-network-01")
                        .param("fileType", "AUTO_DETECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid_events_count").value(4))
                .andExpect(jsonPath("$.error_count").value(0))
                .andReturn();

        EvidenceProcessingResponse networkResponse = objectMapper.readValue(
                networkResult.getResponse().getContentAsString(),
                EvidenceProcessingResponse.class
        );
        allEvents.addAll(networkResponse.getEvents());

        assertThat(allEvents).hasSize(16);

        // 5. Merge all evidence into unified chronological timeline
        MvcResult mergeResult = mockMvc.perform(post("/api/v1/evidence/timeline/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allEvents)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_events").value(16))
                .andExpect(jsonPath("$.case_ids[0]").value("case-alpha"))
                .andReturn();

        TimelineMergeResponse mergedTimeline = objectMapper.readValue(
                mergeResult.getResponse().getContentAsString(),
                TimelineMergeResponse.class
        );

        // Verify strictly ascending chronological timeline across all evidence categories
        List<NormalizedEventDTO> mergedEvents = mergedTimeline.getEvents();
        assertThat(mergedEvents).hasSize(16);

        for (int i = 0; i < mergedEvents.size() - 1; i++) {
            assertThat(mergedEvents.get(i).getTimestamp())
                    .isBeforeOrEqualTo(mergedEvents.get(i + 1).getTimestamp());
        }

        // Verify earliest and latest boundary timestamps match metadata
        assertThat(mergedTimeline.getStartTime()).isEqualTo(mergedEvents.get(0).getTimestamp());
        assertThat(mergedTimeline.getEndTime()).isEqualTo(mergedEvents.get(15).getTimestamp());
    }

    @Test
    @DisplayName("Test malformed_corrupt.csv: Zero silent failures, errors isolated, valid rows preserved")
    void testMalformedCorruptFileHandling() throws Exception {
        byte[] corruptBytes = Files.readAllBytes(new ClassPathResource("samples/malformed_corrupt.csv").getFile().toPath());
        MockMultipartFile corruptFile = new MockMultipartFile("file", "malformed_corrupt.csv", "text/csv", corruptBytes);

        MvcResult result = mockMvc.perform(multipart("/api/v1/evidence/upload-and-process")
                        .file(corruptFile)
                        .param("caseId", "case-beta")
                        .param("sourceEvidenceId", "ev-corrupt-01")
                        .param("fileType", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_processed_rows").value(6))
                .andExpect(jsonPath("$.valid_events_count").value(2)) // user1 and user4
                .andExpect(jsonPath("$.error_count").value(4))        // bad date, blank line, missing ts, duplicate
                .andReturn();

        EvidenceProcessingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                EvidenceProcessingResponse.class
        );

        // Valid events are preserved and sorted
        assertThat(response.getEvents()).hasSize(2);
        assertThat(response.getEvents().get(0).getUser()).isEqualTo("user1");
        assertThat(response.getEvents().get(1).getUser()).isEqualTo("user4");

        // Error report audit contains reasons and raw contents
        assertThat(response.getErrors())
                .anyMatch(err -> err.getReason().contains("Unparseable date: 26-99-2026"))
                .anyMatch(err -> err.getReason().equals("Blank row"))
                .anyMatch(err -> err.getReason().contains("Missing required column: timestamp"))
                .anyMatch(err -> err.getReason().contains("Duplicate event skipped"));
    }
}
