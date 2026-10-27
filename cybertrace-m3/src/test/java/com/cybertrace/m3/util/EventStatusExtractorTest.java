package com.cybertrace.m3.util;

import com.cybertrace.m3.model.NormalizedEventDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EventStatusExtractorTest {

    @Test
    @DisplayName("1. Extracts LOGIN SUCCESS from actual M2 description")
    void testExtractLoginSuccess() {
        String description = "User user1 login attempt: SUCCESS";
        EventStatusExtractor.LoginStatus status = EventStatusExtractor.extractStatus(description);

        assertThat(status).isEqualTo(EventStatusExtractor.LoginStatus.SUCCESS);
        assertThat(EventStatusExtractor.isSuccess(description)).isTrue();
        assertThat(EventStatusExtractor.isFailure(description)).isFalse();
        assertThat(EventStatusExtractor.isUnknown(description)).isFalse();
        assertThat(EventStatusExtractor.extractStatusAsString(description)).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("2. Extracts LOGIN FAILED from actual M2 description")
    void testExtractLoginFailed() {
        String description = "User attacker login attempt: FAILED";
        EventStatusExtractor.LoginStatus status = EventStatusExtractor.extractStatus(description);

        assertThat(status).isEqualTo(EventStatusExtractor.LoginStatus.FAILED);
        assertThat(EventStatusExtractor.isFailure(description)).isTrue();
        assertThat(EventStatusExtractor.isSuccess(description)).isFalse();
        assertThat(EventStatusExtractor.isUnknown(description)).isFalse();
        assertThat(EventStatusExtractor.extractStatusAsString(description)).isEqualTo("FAILED");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "User admin login attempt: success",
            "User root login attempt: Success",
            "User user1 login attempt:   SUCCESS   "
    })
    @DisplayName("3. Extracts SUCCESS with case-insensitivity and extra whitespace")
    void testExtractSuccessTolerant(String description) {
        assertThat(EventStatusExtractor.extractStatus(description))
                .isEqualTo(EventStatusExtractor.LoginStatus.SUCCESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "User admin login attempt: failed",
            "User root login attempt: Failed",
            "User user1 login attempt:   FAILED   "
    })
    @DisplayName("4. Extracts FAILED with case-insensitivity and extra whitespace")
    void testExtractFailedTolerant(String description) {
        assertThat(EventStatusExtractor.extractStatus(description))
                .isEqualTo(EventStatusExtractor.LoginStatus.FAILED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "User user1 visited URL: https://internal-portal.company.com/dashboard",
            "User user1 executed action READ on file C:\\Confidential\\financial_q3.xlsx",
            "Network traffic from 192.168.1.10 to 198.51.100.42:443 (15420 bytes)",
            "User user1 login attempt: INVALID_STATUS",
            "User user1 login attempt: "
    })
    @DisplayName("5. Returns UNKNOWN for non-login and unrecognized descriptions")
    void testExtractUnknownNonLogin(String description) {
        assertThat(EventStatusExtractor.extractStatus(description))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
        assertThat(EventStatusExtractor.isUnknown(description)).isTrue();
        assertThat(EventStatusExtractor.extractStatusAsString(description)).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("6. Returns UNKNOWN for null or empty descriptions")
    void testExtractNullOrEmpty() {
        assertThat(EventStatusExtractor.extractStatus((String) null))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
        assertThat(EventStatusExtractor.extractStatus(""))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
        assertThat(EventStatusExtractor.extractStatus("   "))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
    }

    @Test
    @DisplayName("7. Extracts status from NormalizedEventDTO directly")
    void testExtractFromNormalizedEventDTO() {
        NormalizedEventDTO successEvent = NormalizedEventDTO.builder()
                .eventType("LOGIN")
                .description("User user1 login attempt: SUCCESS")
                .build();

        NormalizedEventDTO failedEvent = NormalizedEventDTO.builder()
                .eventType("LOGIN")
                .description("User user2 login attempt: FAILED")
                .build();

        NormalizedEventDTO browserEvent = NormalizedEventDTO.builder()
                .eventType("BROWSER_ACTIVITY")
                .description("User user1 visited URL: https://google.com")
                .build();

        NormalizedEventDTO nullDescEvent = NormalizedEventDTO.builder()
                .eventType("LOGIN")
                .description(null)
                .build();

        assertThat(EventStatusExtractor.extractStatus(successEvent))
                .isEqualTo(EventStatusExtractor.LoginStatus.SUCCESS);
        assertThat(EventStatusExtractor.extractStatus(failedEvent))
                .isEqualTo(EventStatusExtractor.LoginStatus.FAILED);
        assertThat(EventStatusExtractor.extractStatus(browserEvent))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
        assertThat(EventStatusExtractor.extractStatus(nullDescEvent))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
        assertThat(EventStatusExtractor.extractStatus((NormalizedEventDTO) null))
                .isEqualTo(EventStatusExtractor.LoginStatus.UNKNOWN);
    }
}
