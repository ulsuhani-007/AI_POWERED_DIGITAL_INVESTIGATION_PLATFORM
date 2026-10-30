package com.investigation.model.enums;

public enum CaseStatus {
    OPEN("Open", "#38BDF8"),
    IN_PROGRESS("In Progress", "#F59E0B"),
    CLOSED("Closed", "#10B981"),
    ARCHIVED("Archived", "#64748B");

    private final String displayName;
    private final String colorHex;

    CaseStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static CaseStatus fromString(String text) {
        if (text == null) return OPEN;
        for (CaseStatus status : CaseStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || status.displayName.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return OPEN;
    }
}
