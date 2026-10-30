package com.investigation.graph.model;

import java.util.*;

/**
 * Encapsulates the entire graph topology, adjacency indices, and search methods.
 */
public class GraphData {
    private final Map<String, GraphNode> nodesById = new LinkedHashMap<>();
    private final Set<GraphEdge> edges = new LinkedHashSet<>();
    private final Map<GraphNode, Set<GraphNode>> adjacencyList = new HashMap<>();
    private final Map<GraphNode, Set<GraphEdge>> incidentEdges = new HashMap<>();

    public GraphData() {}

    public void addNode(GraphNode node) {
        if (node == null) return;
        nodesById.put(node.getId(), node);
        adjacencyList.putIfAbsent(node, new HashSet<>());
        incidentEdges.putIfAbsent(node, new HashSet<>());
    }

    public void addEdge(GraphEdge edge) {
        if (edge == null || edge.getSource() == null || edge.getTarget() == null) return;
        addNode(edge.getSource());
        addNode(edge.getTarget());
        edges.add(edge);

        adjacencyList.get(edge.getSource()).add(edge.getTarget());
        adjacencyList.get(edge.getTarget()).add(edge.getSource());

        incidentEdges.get(edge.getSource()).add(edge);
        incidentEdges.get(edge.getTarget()).add(edge);
    }

    public GraphNode getNodeById(String id) {
        return nodesById.get(id);
    }

    public Collection<GraphNode> getNodes() {
        return nodesById.values();
    }

    public Set<GraphEdge> getEdges() {
        return Collections.unmodifiableSet(edges);
    }

    public Set<GraphNode> getNeighbors(GraphNode node) {
        return adjacencyList.getOrDefault(node, Collections.emptySet());
    }

    public Set<GraphEdge> getIncidentEdges(GraphNode node) {
        return incidentEdges.getOrDefault(node, Collections.emptySet());
    }

    public void clear() {
        nodesById.clear();
        edges.clear();
        adjacencyList.clear();
        incidentEdges.clear();
    }

    public int getNodeCount() {
        return nodesById.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }
}
