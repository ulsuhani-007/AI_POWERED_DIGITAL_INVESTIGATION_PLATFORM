package com.investigation.model.enums;

public enum EvidenceStatus {
    UPLOADED("Uploaded", "#38BDF8"),
    PROCESSING("Processing", "#F59E0B"),
    ANALYZED("Analyzed", "#10B981"),
    FAILED("Failed", "#EF4444");

    private final String displayName;
    private final String colorHex;

    EvidenceStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static EvidenceStatus fromString(String text) {
        if (text == null) return UPLOADED;
        for (EvidenceStatus s : EvidenceStatus.values()) {
            if (s.name().equalsIgnoreCase(text) || s.displayName.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return UPLOADED;
    }
}
