package com.investigation.graph.model;

import java.util.Objects;

/**
 * Edge representing an investigative relationship between two nodes.
 */
public class GraphEdge {
    private final String id;
    private final GraphNode source;
    private final GraphNode target;
    private String label;
    private double weight = 1.0;

    private boolean highlighted = false;
    private boolean dimmed = false;

    public GraphEdge(String id, GraphNode source, GraphNode target, String label) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.label = label;
    }

    public String getId() { return id; }
    public GraphNode getSource() { return source; }
    public GraphNode getTarget() { return target; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    public boolean isDimmed() { return dimmed; }
    public void setDimmed(boolean dimmed) { this.dimmed = dimmed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(id, graphEdge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return source.getLabel() + " --[" + label + "]--> " + target.getLabel();
    }
}
