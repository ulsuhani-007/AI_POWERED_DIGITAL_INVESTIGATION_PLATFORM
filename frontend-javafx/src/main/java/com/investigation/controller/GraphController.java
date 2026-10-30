package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.graph.adapter.CaseGraphAdapter;
import com.investigation.graph.model.GraphData;
import com.investigation.graph.model.GraphEdge;
import com.investigation.graph.model.GraphNode;
import com.investigation.graph.model.NodeType;
import com.investigation.graph.view.RelationshipGraphView;
import com.investigation.model.AnalysisResultModel;
import com.investigation.model.CaseModel;
import com.investigation.model.EventModel;
import com.investigation.model.EvidenceModel;
import com.investigation.service.AnalysisService;
import com.investigation.service.EventService;
import com.investigation.service.EvidenceService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private StackPane graphContainer;

    // Controls
    @FXML private TextField nodeSearchField;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button resetViewButton;
    @FXML private ToggleButton physicsToggle;

    // Filter checkboxes
    @FXML private CheckBox filterPerson;
    @FXML private CheckBox filterDevice;
    @FXML private CheckBox filterIp;
    @FXML private CheckBox filterEvent;
    @FXML private CheckBox filterEvidence;
    @FXML private CheckBox filterLocation;

    // Details panel
    @FXML private VBox detailPanel;
    @FXML private Label detailNodeType;
    @FXML private Label detailNodeLabel;
    @FXML private Label detailNodeSubtitle;
    @FXML private Label detailConnectedCount;
    @FXML private VBox dynamicPropertiesBox;
    @FXML private VBox connectedNeighborsBox;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;
    private RelationshipGraphView graphView;
    private final CaseGraphAdapter adapter = new CaseGraphAdapter();
    private GraphData currentGraphData = new GraphData();

    @FXML
    public void initialize() {
        this.graphView = new RelationshipGraphView();
        graphContainer.getChildren().setAll(graphView);

        setupGraphListeners();
        setupFilters();
        refreshData();
    }

    private void showLoading(String message) {
        hideLoading();
        loadingOverlay = UiUtils.createLoadingOverlay(message);
        overlayPane.getChildren().add(loadingOverlay);
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            overlayPane.getChildren().remove(loadingOverlay);
            loadingOverlay = null;
        }
    }

    private void setupGraphListeners() {
        graphView.setOnNodeSelected(this::showNodeDetails);

        nodeSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            graphView.searchAndFocus(newVal);
        });

        zoomInButton.setOnAction(e -> graphView.zoomIn());
        zoomOutButton.setOnAction(e -> graphView.zoomOut());
        resetViewButton.setOnAction(e -> graphView.resetView());

        physicsToggle.setOnAction(e -> {
            graphView.togglePhysics();
            physicsToggle.setText(graphView.isPhysicsEnabled() ? "⏸️ Physics Active" : "▶️ Physics Paused");
        });
    }

    private void setupFilters() {
        setupFilterCheckbox(filterPerson, NodeType.PERSON);
        setupFilterCheckbox(filterDevice, NodeType.DEVICE);
        setupFilterCheckbox(filterIp, NodeType.IP_ADDRESS);
        setupFilterCheckbox(filterEvent, NodeType.EVENT);
        setupFilterCheckbox(filterEvidence, NodeType.EVIDENCE);
        setupFilterCheckbox(filterLocation, NodeType.LOCATION);
    }

    private void setupFilterCheckbox(CheckBox cb, NodeType type) {
        if (cb != null) {
            cb.setSelected(true);
            cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                graphView.setTypeFilter(type, newVal);
            });
        }
    }

    @FXML
    @Override
    public void refreshData() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            try {
                List<CaseModel> allCases = ServiceFactory.getInstance().getCaseService().getAllCases();
                if (!allCases.isEmpty()) {
                    activeCase = allCases.get(0);
                    AppConfig.getInstance().setActiveCase(activeCase);
                }
            } catch (Exception ignored) {}
        }

        if (activeCase == null) {
            activeCaseHeaderLabel.setText("NO ACTIVE CASE SELECTED");
            showNodeDetails(null);
            graphView.setGraphData(new GraphData());
            return;
        }

        final CaseModel currentCase = activeCase;
        activeCaseHeaderLabel.setText("RELATIONSHIP TOPOLOGY FOR " + currentCase.getCaseNumber() + " : " + currentCase.getTitle());

        showLoading("Synthesizing multi-entity relationship graph...");

        Task<GraphData> task = new Task<>() {
            @Override
            protected GraphData call() throws Exception {
                EvidenceService evService = ServiceFactory.getInstance().getEvidenceService();
                EventService eventService = ServiceFactory.getInstance().getEventService();
                AnalysisService analysisService = ServiceFactory.getInstance().getAnalysisService();

                List<EvidenceModel> evidence = evService.getEvidenceByCase(currentCase.getId());
                List<EventModel> events = eventService.getEventsByCase(currentCase.getId());
                AnalysisResultModel analysis = analysisService.getAnalysisByCase(currentCase.getId());

                return adapter.buildGraph(
                        currentCase,
                        evidence,
                        events,
                        analysis != null ? analysis.getFindings() : null
                );
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            this.currentGraphData = task.getValue();
            graphView.setGraphData(currentGraphData);
            showNodeDetails(null);
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Generate Graph", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void showNodeDetails(GraphNode node) {
        if (node == null) {
            detailNodeType.setText("No Node Selected");
            detailNodeType.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
            detailNodeLabel.setText("Click any node on the graph canvas");
            detailNodeSubtitle.setText("Hover or click a node to highlight relationships.");
            detailConnectedCount.setText("0 Edges");
            dynamicPropertiesBox.getChildren().clear();
            connectedNeighborsBox.getChildren().clear();
            return;
        }

        NodeType type = node.getType();
        detailNodeType.setText(type.getIconSymbol() + "  " + type.getDisplayName().toUpperCase());
        detailNodeType.setStyle("-fx-text-fill: " + type.getColorHex() + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        detailNodeLabel.setText(node.getLabel());
        detailNodeSubtitle.setText(node.getSubtitle() != null ? node.getSubtitle() : "Entity ID: " + node.getId());

        Set<GraphEdge> edges = currentGraphData.getIncidentEdges(node);
        detailConnectedCount.setText(edges.size() + " Connected Relationships");

        // Dynamic properties
        dynamicPropertiesBox.getChildren().clear();
        for (Map.Entry<String, Object> entry : node.getProperties().entrySet()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label keyLabel = new Label(entry.getKey() + ":");
            keyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-min-width: 90px;");

            Label valLabel = new Label(String.valueOf(entry.getValue()));
            valLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #F8FAFC; -fx-font-weight: bold;");
            valLabel.setWrapText(true);

            row.getChildren().addAll(keyLabel, valLabel);
            dynamicPropertiesBox.getChildren().add(row);
        }

        // Connected Neighbors List
        connectedNeighborsBox.getChildren().clear();
        for (GraphEdge edge : edges) {
            GraphNode neighbor = edge.getSource().equals(node) ? edge.getTarget() : edge.getSource();

            HBox item = new HBox(6);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-background-color: #090E1A; -fx-padding: 6px 8px; -fx-background-radius: 4px; -fx-cursor: hand;");

            Label relBadge = new Label(edge.getLabel());
            relBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-padding: 2px 4px; -fx-background-radius: 3px;");

            Label neighborLabel = new Label(neighbor.getLabel());
            neighborLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #F8FAFC;");

            item.getChildren().addAll(relBadge, neighborLabel);

            item.setOnMouseClicked(e -> {
                graphView.searchAndFocus(neighbor.getLabel());
            });

            connectedNeighborsBox.getChildren().add(item);
        }
    }
}
