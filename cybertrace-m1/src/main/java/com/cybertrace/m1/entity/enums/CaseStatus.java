package com.cybertrace.m1.entity.enums;

public enum CaseStatus {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    CLOSED("Closed"),
    ARCHIVED("Archived");

    private final String displayName;

    CaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CaseStatus fromString(String text) {
        if (text == null || text.isBlank()) {
            return OPEN;
        }
        for (CaseStatus status : CaseStatus.values()) {
            if (status.name().equalsIgnoreCase(text.trim()) ||
                    status.displayName.equalsIgnoreCase(text.trim()) ||
                    status.name().replace("_", " ").equalsIgnoreCase(text.trim())) {
                return status;
            }
        }
        return OPEN;
    }
}

