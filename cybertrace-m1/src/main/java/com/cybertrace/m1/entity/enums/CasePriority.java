package com.cybertrace.m1.entity.enums;

public enum CasePriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String displayName;

    CasePriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CasePriority fromString(String text) {
        if (text == null || text.isBlank()) {
            return MEDIUM;
        }
        for (CasePriority priority : CasePriority.values()) {
            if (priority.name().equalsIgnoreCase(text.trim()) ||
                    priority.displayName.equalsIgnoreCase(text.trim())) {
                return priority;
            }
        }
        return MEDIUM;
    }
}

