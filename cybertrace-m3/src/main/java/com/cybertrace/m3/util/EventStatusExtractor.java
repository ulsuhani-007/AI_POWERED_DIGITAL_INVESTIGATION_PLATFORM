package com.cybertrace.m3.util;

import com.cybertrace.m3.model.NormalizedEventDTO;

/**
 * Utility to extract login outcome from M2's standardized description format:
 * "User {user} login attempt: SUCCESS" or "User {user} login attempt: FAILED".
 */
public final class EventStatusExtractor {

    public static final String LOGIN_ATTEMPT_PREFIX = "login attempt:";

    /**
     * Enumeration of recognized login statuses.
     */
    public enum LoginStatus {
        SUCCESS,
        FAILED,
        UNKNOWN
    }

    private EventStatusExtractor() {
        // Utility class
    }

    /**
     * Extracts the login status from an event description string.
     *
     * @param description the description string from M2
     * @return {@link LoginStatus#SUCCESS}, {@link LoginStatus#FAILED}, or {@link LoginStatus#UNKNOWN}
     */
    public static LoginStatus extractStatus(String description) {
        if (description == null || description.isBlank()) {
            return LoginStatus.UNKNOWN;
        }

        String trimmed = description.trim();
        int prefixIndex = trimmed.toLowerCase().lastIndexOf(LOGIN_ATTEMPT_PREFIX);
        if (prefixIndex == -1) {
            return LoginStatus.UNKNOWN;
        }

        String statusPart = trimmed.substring(prefixIndex + LOGIN_ATTEMPT_PREFIX.length()).trim();

        if ("SUCCESS".equalsIgnoreCase(statusPart)) {
            return LoginStatus.SUCCESS;
        } else if ("FAILED".equalsIgnoreCase(statusPart)) {
            return LoginStatus.FAILED;
        }

        return LoginStatus.UNKNOWN;
    }

    /**
     * Extracts login status directly from a NormalizedEventDTO.
     *
     * @param event the normalized event
     * @return {@link LoginStatus}
     */
    public static LoginStatus extractStatus(NormalizedEventDTO event) {
        if (event == null) {
            return LoginStatus.UNKNOWN;
        }
        return extractStatus(event.getDescription());
    }

    /**
     * Returns the extracted status as a normalized string: "SUCCESS", "FAILED", or "UNKNOWN".
     *
     * @param description the description string
     * @return status string representation
     */
    public static String extractStatusAsString(String description) {
        return extractStatus(description).name();
    }

    /**
     * Checks if the event indicates a successful login attempt.
     */
    public static boolean isSuccess(String description) {
        return extractStatus(description) == LoginStatus.SUCCESS;
    }

    /**
     * Checks if the event indicates a failed login attempt.
     */
    public static boolean isFailure(String description) {
        return extractStatus(description) == LoginStatus.FAILED;
    }

    /**
     * Checks if the event status is unknown or non-login.
     */
    public static boolean isUnknown(String description) {
        return extractStatus(description) == LoginStatus.UNKNOWN;
    }
}
