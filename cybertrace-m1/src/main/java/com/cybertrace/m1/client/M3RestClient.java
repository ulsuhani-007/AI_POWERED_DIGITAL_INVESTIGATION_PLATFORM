package com.cybertrace.m1.client;

import com.cybertrace.m1.dto.AnalysisDTO;
import com.cybertrace.m1.dto.NormalizedEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class M3RestClient implements M3Client {

    private static final Logger log = LoggerFactory.getLogger(M3RestClient.class);

    private final RestClient restClient;
    private final String m3BaseUrl;

    @Autowired
    public M3RestClient(
            @Value("${cybertrace.m3.url:http://localhost:8082}") String m3BaseUrl,
            @Value("${cybertrace.integration.timeout-seconds:10}") int timeoutSeconds
    ) {
        this.m3BaseUrl = m3BaseUrl.endsWith("/") ? m3BaseUrl.substring(0, m3BaseUrl.length() - 1) : m3BaseUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // Constructor for testing with custom RestClient
    public M3RestClient(RestClient restClient, String m3BaseUrl) {
        this.restClient = restClient;
        this.m3BaseUrl = m3BaseUrl.endsWith("/") ? m3BaseUrl.substring(0, m3BaseUrl.length() - 1) : m3BaseUrl;
    }

    @Override
    public Optional<AnalysisDTO> analyzeEvents(String caseId, List<NormalizedEventDTO> events) {
        if (events == null || events.isEmpty()) {
            log.info("No events to send to M3 for case {}", caseId);
            return Optional.empty();
        }

        try {
            String targetUri = m3BaseUrl + "/api/v1/analysis/analyze";
            log.info("Sending {} normalized events to M3 for analysis at {}", events.size(), targetUri);

            AnalysisDTO report = restClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(events)
                    .retrieve()
                    .body(AnalysisDTO.class);

            if (report != null) {
                report.setCaseId(caseId);
                log.info("M3 analysis succeeded for case {}: {} events, {} anomalies flagged",
                        caseId, report.getTotalEvents(), report.getTotalAnomalies());
                return Optional.of(report);
            } else {
                log.warn("M3 returned empty analysis body for case {}", caseId);
                return Optional.empty();
            }

        } catch (ResourceAccessException e) {
            log.error("M3 service is unavailable or timed out at {}: {}", m3BaseUrl, e.getMessage());
            return Optional.empty();
        } catch (RestClientResponseException e) {
            log.error("M3 returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error communicating with M3: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
