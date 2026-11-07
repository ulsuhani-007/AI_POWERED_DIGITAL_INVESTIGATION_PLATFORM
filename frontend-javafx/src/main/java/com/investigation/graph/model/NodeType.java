package com.investigation.graph.model;

import javafx.scene.paint.Color;

public enum NodeType {
    PERSON("Person", "#2563EB", "👤", Color.web("#2563EB")),
    DEVICE("Device", "#0284C7", "💻", Color.web("#0284C7")),
    IP_ADDRESS("IP Address", "#0284C7", "🌐", Color.web("#0284C7")),
    EVENT("Event", "#DC2626", "⚡", Color.web("#DC2626")),
    EVIDENCE("Evidence", "#1E40AF", "📁", Color.web("#1E40AF")),
    FILE("File", "#475569", "📄", Color.web("#475569")),
    URL("Web URL", "#0284C7", "🔗", Color.web("#0284C7")),
    LOCATION("Location", "#D97706", "📍", Color.web("#D97706"));

    private final String displayName;
    private final String colorHex;
    private final String iconSymbol;
    private final Color fxColor;

    NodeType(String displayName, String colorHex, String iconSymbol, Color fxColor) {
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.iconSymbol = iconSymbol;
        this.fxColor = fxColor;
    }

    public String getDisplayName() { return displayName; }
    public String getColorHex() { return colorHex; }
    public String getIconSymbol() { return iconSymbol; }
    public Color getFxColor() { return fxColor; }
}
