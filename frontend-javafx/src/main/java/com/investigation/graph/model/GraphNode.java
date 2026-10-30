package com.investigation.graph.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Node in the digital investigation relationship graph.
 */
public class GraphNode {
    private final String id;
    private String label;
    private NodeType type;
    private String subtitle;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    // Physical coordinates and velocity for force-directed layout
    private double x;
    private double y;
    private double vx;
    private double vy;
    private double radius = 24.0;
    private boolean fixed = false;

    // Visual interactive states
    private boolean selected = false;
    private boolean highlighted = false;
    private boolean dimmed = false;

    public GraphNode(String id, String label, NodeType type, String subtitle) {
        this.id = id;
        this.label = label;
        this.type = type != null ? type : NodeType.EVENT;
        this.subtitle = subtitle;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public Map<String, Object> getProperties() { return properties; }
    public void addProperty(String key, Object value) { this.properties.put(key, value); }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getVx() { return vx; }
    public void setVx(double vx) { this.vx = vx; }

    public double getVy() { return vy; }
    public void setVy(double vy) { this.vy = vy; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public boolean isFixed() { return fixed; }
    public void setFixed(boolean fixed) { this.fixed = fixed; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    public boolean isDimmed() { return dimmed; }
    public void setDimmed(boolean dimmed) { this.dimmed = dimmed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(id, graphNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return label + " (" + type + ")";
    }
}
