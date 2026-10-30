package com.investigation.model.enums;

public enum CasePriority {
    LOW("Low", "#94A3B8"),
    MEDIUM("Medium", "#38BDF8"),
    HIGH("High", "#F59E0B"),
    CRITICAL("Critical", "#EF4444");

    private final String displayName;
    private final String colorHex;

    CasePriority(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static CasePriority fromString(String text) {
        if (text == null) return MEDIUM;
        for (CasePriority p : CasePriority.values()) {
            if (p.name().equalsIgnoreCase(text) || p.displayName.equalsIgnoreCase(text)) {
                return p;
            }
        }
        return MEDIUM;
    }
}
