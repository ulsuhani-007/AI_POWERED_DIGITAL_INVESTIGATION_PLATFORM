package com.investigation.graph.model;

import javafx.scene.paint.Color;

public enum NodeType {
    PERSON("Person / User", "#00F0FF", "👤", Color.web("#00F0FF")),
    DEVICE("Endpoint / Device", "#A855F7", "💻", Color.web("#A855F7")),
    IP_ADDRESS("IP Address", "#F59E0B", "🌐", Color.web("#F59E0B")),
    EVENT("Investigation Event", "#3B82F6", "⚡", Color.web("#3B82F6")),
    EVIDENCE("Evidence Source", "#10B981", "📁", Color.web("#10B981")),
    LOCATION("Geographic Location", "#F43F5E", "📍", Color.web("#F43F5E"));

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
