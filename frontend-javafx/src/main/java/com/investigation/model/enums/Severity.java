package com.investigation.model.enums;

public enum Severity {
    CRITICAL("CRITICAL", "#EF4444", "#7F1D1D"),
    HIGH("HIGH", "#F97316", "#7C2D12"),
    MEDIUM("MEDIUM", "#EAB308", "#713F12"),
    LOW("LOW", "#06B6D4", "#164E63"),
    INFO("INFO", "#64748B", "#1E293B");

    private final String displayName;
    private final String badgeColorHex;
    private final String bgDarkHex;

    Severity(String displayName, String badgeColorHex, String bgDarkHex) {
        this.displayName = displayName;
        this.badgeColorHex = badgeColorHex;
        this.bgDarkHex = bgDarkHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeColorHex() {
        return badgeColorHex;
    }

    public String getBgDarkHex() {
        return bgDarkHex;
    }

    public static Severity fromString(String text) {
        if (text == null) return INFO;
        for (Severity s : Severity.values()) {
            if (s.name().equalsIgnoreCase(text) || s.displayName.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return INFO;
    }
}
