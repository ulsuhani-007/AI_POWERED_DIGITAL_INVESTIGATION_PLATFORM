package com.cybertrace.m1.entity.enums;

public enum EvidenceStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    PROCESSED("Processed"),
    ANALYZED("Analyzed"),
    FAILED("Failed");

    private final String displayName;

    EvidenceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EvidenceStatus fromString(String text) {
        if (text == null || text.isBlank()) {
            return PENDING;
        }
        for (EvidenceStatus status : EvidenceStatus.values()) {
            if (status.name().equalsIgnoreCase(text.trim()) ||
                    status.displayName.equalsIgnoreCase(text.trim())) {
                return status;
            }
        }
        return PENDING;
    }
}

