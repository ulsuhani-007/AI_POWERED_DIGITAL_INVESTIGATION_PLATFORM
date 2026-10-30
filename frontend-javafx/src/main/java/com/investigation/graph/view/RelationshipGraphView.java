package com.investigation.graph.view;

import com.investigation.graph.model.GraphData;
import com.investigation.graph.model.GraphEdge;
import com.investigation.graph.model.GraphNode;
import com.investigation.graph.model.NodeType;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.util.function.Consumer;

/**
 * High-performance, interactive JavaFX Relationship Graph visualization component.
 * Features: Force-directed physics layout, pan & zoom, node selection,
 * connected edge/neighbor highlighting, and futuristic cyber aesthetic.
 */
public class RelationshipGraphView extends Pane {

    private final Canvas canvas;
    private GraphData graphData = new GraphData();

    // Viewport transform
    private double panX = 0;
    private double panY = 0;
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    // Mouse interaction state
    private double lastMouseX;
    private double lastMouseY;
    private GraphNode draggedNode = null;
    private GraphNode hoveredNode = null;
    private GraphNode selectedNode = null;

    // Selection callback
    private Consumer<GraphNode> onNodeSelectedCallback;

    // Physics simulation
    private final AnimationTimer animationTimer;
    private boolean physicsEnabled = true;
    private boolean needsRedraw = true;
    private double totalEnergy = 100.0;

    // Filter by node type
    private final Set<NodeType> activeTypeFilters = EnumSet.allOf(NodeType.class);

    // Fonts
    private final Font labelFont = Font.font("System", FontWeight.SEMI_BOLD, 11);
    private final Font subtitleFont = Font.font("System", FontWeight.NORMAL, 9);
    private final Font edgeFont = Font.font("System", FontWeight.MEDIUM, 9);
    private final Font iconFont = Font.font("System", FontWeight.BOLD, 15);

    public RelationshipGraphView() {
        this.canvas = new Canvas();
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, oldVal, newVal) -> {
            needsRedraw = true;
            if (panX == 0 && panY == 0 && newVal.doubleValue() > 0) {
                resetView();
            }
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            needsRedraw = true;
            if (panX == 0 && panY == 0 && newVal.doubleValue() > 0) {
                resetView();
            }
        });

        setupMouseHandlers();

        this.animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (physicsEnabled && totalEnergy > 0.05) {
                    stepPhysics();
                    render();
                } else if (needsRedraw) {
                    render();
                    needsRedraw = false;
                }
            }
        };
        animationTimer.start();
    }

    public void setGraphData(GraphData graphData) {
        this.graphData = graphData != null ? graphData : new GraphData();
        this.selectedNode = null;
        this.hoveredNode = null;
        this.draggedNode = null;

        // Spread initial layout in a circle
        int count = this.graphData.getNodeCount();
        int i = 0;
        double radius = Math.min(300, Math.max(120, count * 20));
        for (GraphNode node : this.graphData.getNodes()) {
            double angle = (2 * Math.PI * i) / Math.max(1, count);
            node.setX(radius * Math.cos(angle) + (Math.random() - 0.5) * 50);
            node.setY(radius * Math.sin(angle) + (Math.random() - 0.5) * 50);
            node.setVx(0);
            node.setVy(0);
            i++;
        }

        totalEnergy = 100.0;
        resetView();
        needsRedraw = true;
    }

    public void setOnNodeSelected(Consumer<GraphNode> callback) {
        this.onNodeSelectedCallback = callback;
    }

    public void setTypeFilter(NodeType type, boolean visible) {
        if (visible) {
            activeTypeFilters.add(type);
        } else {
            activeTypeFilters.remove(type);
        }
        needsRedraw = true;
    }

    public void resetView() {
        double w = getWidth() > 0 ? getWidth() : 800;
        double h = getHeight() > 0 ? getHeight() : 600;
        panX = w / 2.0;
        panY = h / 2.0;
        zoom.set(1.0);
        needsRedraw = true;
    }

    public void zoomIn() {
        setZoomCentered(zoom.get() * 1.25);
    }

    public void zoomOut() {
        setZoomCentered(zoom.get() / 1.25);
    }

    private void setZoomCentered(double newZoom) {
        double clamped = Math.max(0.25, Math.min(3.5, newZoom));
        double w = getWidth() / 2.0;
        double h = getHeight() / 2.0;

        double worldX = (w - panX) / zoom.get();
        double worldY = (h - panY) / zoom.get();

        zoom.set(clamped);
        panX = w - worldX * zoom.get();
        panY = h - worldY * zoom.get();
        needsRedraw = true;
    }

    public void togglePhysics() {
        this.physicsEnabled = !this.physicsEnabled;
        if (physicsEnabled) {
            totalEnergy = 50.0;
        }
    }

    public boolean isPhysicsEnabled() {
        return physicsEnabled;
    }

    public void searchAndFocus(String query) {
        if (query == null || query.isBlank()) {
            clearHighlighting();
            return;
        }
        String q = query.trim().toLowerCase();
        for (GraphNode node : graphData.getNodes()) {
            if (node.getLabel().toLowerCase().contains(q) ||
                    (node.getSubtitle() != null && node.getSubtitle().toLowerCase().contains(q))) {
                selectNode(node);
                panX = getWidth() / 2.0 - node.getX() * zoom.get();
                panY = getHeight() / 2.0 - node.getY() * zoom.get();
                needsRedraw = true;
                break;
            }
        }
    }

    private void setupMouseHandlers() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();

            if (e.getButton() == MouseButton.PRIMARY) {
                double worldX = (e.getX() - panX) / zoom.get();
                double worldY = (e.getY() - panY) / zoom.get();
                draggedNode = hitTestNode(worldX, worldY);
                if (draggedNode != null) {
                    draggedNode.setFixed(true);
                    totalEnergy = 20.0;
                }
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            if (draggedNode != null) {
                draggedNode.setX((e.getX() - panX) / zoom.get());
                draggedNode.setY((e.getY() - panY) / zoom.get());
                totalEnergy = 20.0;
                needsRedraw = true;
            } else {
                panX += dx;
                panY += dy;
                needsRedraw = true;
            }

            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (draggedNode != null) {
                draggedNode.setFixed(false);
                draggedNode = null;
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && !e.isStillSincePress()) {
                return; // Was a drag, not a clean click
            }
            double worldX = (e.getX() - panX) / zoom.get();
            double worldY = (e.getY() - panY) / zoom.get();
            GraphNode clicked = hitTestNode(worldX, worldY);

            if (clicked != null) {
                selectNode(clicked);
            } else {
                clearHighlighting();
                selectedNode = null;
                if (onNodeSelectedCallback != null) {
                    onNodeSelectedCallback.accept(null);
                }
            }
            needsRedraw = true;
        });

        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            double worldX = (e.getX() - panX) / zoom.get();
            double worldY = (e.getY() - panY) / zoom.get();
            GraphNode hovered = hitTestNode(worldX, worldY);

            if (hovered != hoveredNode) {
                hoveredNode = hovered;
                if (selectedNode == null) {
                    if (hoveredNode != null) {
                        highlightNeighbors(hoveredNode);
                    } else {
                        clearHighlighting();
                    }
                }
                needsRedraw = true;
            }
        });

        canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY();
            if (delta == 0) return;

            double factor = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoom.get();
            double newZoom = Math.max(0.25, Math.min(3.5, oldZoom * factor));

            double mouseX = e.getX();
            double mouseY = e.getY();

            double worldX = (mouseX - panX) / oldZoom;
            double worldY = (mouseY - panY) / oldZoom;

            zoom.set(newZoom);
            panX = mouseX - worldX * newZoom;
            panY = mouseY - worldY * newZoom;

            needsRedraw = true;
        });
    }

    private GraphNode hitTestNode(double worldX, double worldY) {
        for (GraphNode node : graphData.getNodes()) {
            if (!activeTypeFilters.contains(node.getType())) continue;
            double dx = node.getX() - worldX;
            double dy = node.getY() - worldY;
            if (Math.sqrt(dx * dx + dy * dy) <= node.getRadius() + 4) {
                return node;
            }
        }
        return null;
    }

    private void selectNode(GraphNode node) {
        this.selectedNode = node;
        highlightNeighbors(node);
        if (onNodeSelectedCallback != null) {
            onNodeSelectedCallback.accept(node);
        }
    }

    private void highlightNeighbors(GraphNode centerNode) {
        Set<GraphNode> neighbors = graphData.getNeighbors(centerNode);
        Set<GraphEdge> edges = graphData.getIncidentEdges(centerNode);

        for (GraphNode n : graphData.getNodes()) {
            if (n.equals(centerNode) || neighbors.contains(n)) {
                n.setHighlighted(true);
                n.setDimmed(false);
            } else {
                n.setHighlighted(false);
                n.setDimmed(true);
            }
        }

        for (GraphEdge edge : graphData.getEdges()) {
            if (edges.contains(edge)) {
                edge.setHighlighted(true);
                edge.setDimmed(false);
            } else {
                edge.setHighlighted(false);
                edge.setDimmed(true);
            }
        }
    }

    private void clearHighlighting() {
        for (GraphNode n : graphData.getNodes()) {
            n.setHighlighted(false);
            n.setDimmed(false);
            n.setSelected(false);
        }
        for (GraphEdge edge : graphData.getEdges()) {
            edge.setHighlighted(false);
            edge.setDimmed(false);
        }
    }

    /**
     * Executes one iteration of force-directed physics.
     */
    private void stepPhysics() {
        List<GraphNode> nodes = new ArrayList<>(graphData.getNodes());
        int n = nodes.size();
        if (n == 0) return;

        double repulsion = 12000.0;
        double springK = 0.04;
        double targetLength = 140.0;
        double centerAttraction = 0.015;
        double damping = 0.82;

        totalEnergy = 0;

        // 1. Repulsion between all pairs
        for (int i = 0; i < n; i++) {
            GraphNode n1 = nodes.get(i);
            if (!activeTypeFilters.contains(n1.getType())) continue;

            for (int j = i + 1; j < n; j++) {
                GraphNode n2 = nodes.get(j);
                if (!activeTypeFilters.contains(n2.getType())) continue;

                double dx = n2.getX() - n1.getX();
                double dy = n2.getY() - n1.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < 1.0) dist = 1.0;

                double force = repulsion / (dist * dist);
                double fx = (dx / dist) * force;
                double fy = (dy / dist) * force;

                if (!n1.isFixed()) {
                    n1.setVx(n1.getVx() - fx);
                    n1.setVy(n1.getVy() - fy);
                }
                if (!n2.isFixed()) {
                    n2.setVx(n2.getVx() + fx);
                    n2.setVy(n2.getVy() + fy);
                }
            }
        }

        // 2. Spring attraction along edges
        for (GraphEdge edge : graphData.getEdges()) {
            GraphNode s = edge.getSource();
            GraphNode t = edge.getTarget();
            if (!activeTypeFilters.contains(s.getType()) || !activeTypeFilters.contains(t.getType())) continue;

            double dx = t.getX() - s.getX();
            double dy = t.getY() - s.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1.0) dist = 1.0;

            double displacement = dist - targetLength;
            double force = springK * displacement;

            double fx = (dx / dist) * force;
            double fy = (dy / dist) * force;

            if (!s.isFixed()) {
                s.setVx(s.getVx() + fx);
                s.setVy(s.getVy() + fy);
            }
            if (!t.isFixed()) {
                t.setVx(t.getVx() - fx);
                t.setVy(t.getVy() - fy);
            }
        }

        // 3. Center gravitation and velocity integration
        for (GraphNode node : nodes) {
            if (node.isFixed()) continue;
            if (!activeTypeFilters.contains(node.getType())) continue;

            node.setVx((node.getVx() - node.getX() * centerAttraction) * damping);
            node.setVy((node.getVy() - node.getY() * centerAttraction) * damping);

            node.setX(node.getX() + node.getVx());
            node.setY(node.getY() + node.getVy());

            totalEnergy += (node.getVx() * node.getVx() + node.getVy() * node.getVy());
        }
    }

    /**
     * Renders the complete graph with cyber dark styling.
     */
    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 0 || height <= 0) return;

        // Background
        gc.setFill(Color.web("#090D16"));
        gc.fillRect(0, 0, width, height);

        // Grid lines (futuristic coordinate mesh)
        drawCyberGrid(gc, width, height);

        gc.save();
        gc.translate(panX, panY);
        gc.scale(zoom.get(), zoom.get());

        // 1. Draw Edges
        for (GraphEdge edge : graphData.getEdges()) {
            if (!activeTypeFilters.contains(edge.getSource().getType()) ||
                    !activeTypeFilters.contains(edge.getTarget().getType())) {
                continue;
            }
            drawEdge(gc, edge);
        }

        // 2. Draw Nodes
        for (GraphNode node : graphData.getNodes()) {
            if (!activeTypeFilters.contains(node.getType())) continue;
            drawNode(gc, node);
        }

        gc.restore();
    }

    private void drawCyberGrid(GraphicsContext gc, double width, double height) {
        gc.setStroke(Color.web("#10192A"));
        gc.setLineWidth(1.0);

        double gridSize = 40.0 * zoom.get();
        if (gridSize < 15) gridSize = 15;

        double offsetX = panX % gridSize;
        double offsetY = panY % gridSize;

        for (double x = offsetX; x < width; x += gridSize) {
            gc.strokeLine(x, 0, x, height);
        }
        for (double y = offsetY; y < height; y += gridSize) {
            gc.strokeLine(0, y, width, y);
        }
    }

    private void drawEdge(GraphicsContext gc, GraphEdge edge) {
        GraphNode s = edge.getSource();
        GraphNode t = edge.getTarget();

        double x1 = s.getX();
        double y1 = s.getY();
        double x2 = t.getX();
        double y2 = t.getY();

        double opacity = edge.isDimmed() ? 0.15 : (edge.isHighlighted() ? 1.0 : 0.45);
        Color strokeColor = edge.isHighlighted() ? Color.web("#00F0FF") : Color.web("#38BDF8");

        gc.setStroke(Color.color(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(), opacity));
        gc.setLineWidth(edge.isHighlighted() ? 2.5 : 1.2);
        gc.strokeLine(x1, y1, x2, y2);

        // Draw edge relationship label at midpoint
        if (!edge.isDimmed() && (edge.isHighlighted() || zoom.get() > 0.8)) {
            double midX = (x1 + x2) / 2.0;
            double midY = (y1 + y2) / 2.0;

            String label = edge.getLabel();
            if (label != null && !label.isBlank()) {
                gc.setFont(edgeFont);
                double textWidth = label.length() * 5.5 + 8;
                double textHeight = 14;

                gc.setFill(Color.web("#0B132B", opacity > 0.7 ? 0.9 : 0.6));
                gc.fillRoundRect(midX - textWidth / 2.0, midY - textHeight / 2.0, textWidth, textHeight, 4, 4);

                gc.setStroke(Color.web("#1E293B", opacity));
                gc.setLineWidth(0.8);
                gc.strokeRoundRect(midX - textWidth / 2.0, midY - textHeight / 2.0, textWidth, textHeight, 4, 4);

                gc.setFill(Color.web("#94A3B8", opacity));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(label, midX, midY + 4);
            }
        }
    }

    private void drawNode(GraphicsContext gc, GraphNode node) {
        double x = node.getX();
        double y = node.getY();
        double r = node.getRadius();

        Color baseColor = node.getType().getFxColor();
        double opacity = node.isDimmed() ? 0.20 : 1.0;

        // Glowing outer halo for highlighted or selected node
        if (node.isHighlighted() || node.equals(selectedNode)) {
            gc.save();
            gc.setStroke(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.35 * opacity));
            gc.setLineWidth(10.0);
            gc.strokeOval(x - r - 4, y - r - 4, (r + 4) * 2, (r + 4) * 2);
            gc.restore();
        }

        // Outer border ring
        gc.setStroke(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity));
        gc.setLineWidth(node.equals(selectedNode) ? 3.0 : (node.isHighlighted() ? 2.2 : 1.5));

        // Radial fill gradient inside node
        LinearGradient grad = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1E293B", opacity)),
                new Stop(1, Color.web("#0F172A", opacity))
        );
        gc.setFill(grad);
        gc.fillOval(x - r, y - r, r * 2, r * 2);
        gc.strokeOval(x - r, y - r, r * 2, r * 2);

        // Icon symbol in center
        gc.setFont(iconFont);
        gc.setFill(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(node.getType().getIconSymbol(), x, y + 5);

        // Node Label below node
        if (!node.isDimmed()) {
            gc.setFont(labelFont);
            gc.setFill(Color.web("#F8FAFC", opacity));
            gc.fillText(node.getLabel(), x, y + r + 14);

            if (node.getSubtitle() != null && !node.getSubtitle().isBlank()) {
                gc.setFont(subtitleFont);
                gc.setFill(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.85 * opacity));
                gc.fillText(node.getSubtitle(), x, y + r + 26);
            }
        }
    }
}
