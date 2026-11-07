package com.cybertrace.m1.client;

import com.cybertrace.m1.dto.M2EvidenceProcessingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@Component
public class M2RestClient implements M2Client {

    private static final Logger log = LoggerFactory.getLogger(M2RestClient.class);

    private final RestClient restClient;
    private final String m2BaseUrl;

    @Autowired
    public M2RestClient(
            @Value("${cybertrace.m2.url:http://localhost:8081}") String m2BaseUrl,
            @Value("${cybertrace.integration.timeout-seconds:10}") int timeoutSeconds
    ) {
        this.m2BaseUrl = m2BaseUrl.endsWith("/") ? m2BaseUrl.substring(0, m2BaseUrl.length() - 1) : m2BaseUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // Constructor for testing with custom RestClient
    public M2RestClient(RestClient restClient, String m2BaseUrl) {
        this.restClient = restClient;
        this.m2BaseUrl = m2BaseUrl.endsWith("/") ? m2BaseUrl.substring(0, m2BaseUrl.length() - 1) : m2BaseUrl;
    }

    @Override
    public Optional<M2EvidenceProcessingResponse> processEvidence(String caseId, String sourceEvidenceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Cannot forward null or empty file to M2");
            return Optional.empty();
        }

        try {
            byte[] fileBytes = file.getBytes();
            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence.csv";

            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFileName;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("caseId", caseId);
            body.add("sourceEvidenceId", sourceEvidenceId);
            body.add("fileType", "AUTO_DETECT");
            body.add("file", fileResource);

            String targetUri = m2BaseUrl + "/api/v1/evidence/upload-and-process";
            log.info("Forwarding evidence file '{}' ({}) to M2 at {}", originalFileName, sourceEvidenceId, targetUri);

            M2EvidenceProcessingResponse response = restClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(M2EvidenceProcessingResponse.class);

            if (response != null) {
                log.info("M2 successfully processed evidence {}: {} valid events, {} errors",
                        sourceEvidenceId, response.getValidEventsCount(), response.getErrorCount());
                return Optional.of(response);
            } else {
                log.warn("M2 returned empty response body for evidence {}", sourceEvidenceId);
                return Optional.empty();
            }

        } catch (ResourceAccessException e) {
            log.error("M2 service is unavailable or timed out at {}: {}", m2BaseUrl, e.getMessage());
            return Optional.empty();
        } catch (RestClientResponseException e) {
            log.error("M2 returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (IOException e) {
            log.error("Failed to read file bytes for M2 forwarding: {}", e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error communicating with M2: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
