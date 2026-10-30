package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.AnalysisResultModel;
import com.investigation.model.CaseModel;
import com.investigation.model.FindingModel;
import com.investigation.model.enums.Severity;
import com.investigation.service.AnalysisService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class AnomaliesController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private Label mockNoticeLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> severityFilterComboBox;
    @FXML private Label anomalyCountLabel;

    @FXML private TableView<FindingModel> anomaliesTable;
    @FXML private TableColumn<FindingModel, String> colSeverity;
    @FXML private TableColumn<FindingModel, String> colScore;
    @FXML private TableColumn<FindingModel, String> colEventType;
    @FXML private TableColumn<FindingModel, String> colReason;
    @FXML private TableColumn<FindingModel, String> colMethod;
    @FXML private TableColumn<FindingModel, String> colEventId;
    @FXML private TableColumn<FindingModel, String> colTimestamp;

    // Side detail panel
    @FXML private VBox detailPanel;
    @FXML private HBox detailSeverityBadge;
    @FXML private Label detailScore;
    @FXML private ProgressBar detailScoreBar;
    @FXML private Label detailEventType;
    @FXML private Label detailTimestamp;
    @FXML private Label detailEventId;
    @FXML private Label detailMethod;
    @FXML private Label detailAffectedEntity;
    @FXML private TextArea detailReasonArea;
    @FXML private TextArea detailSuggestedActionArea;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;

    private final ObservableList<FindingModel> masterFindings = FXCollections.observableArrayList();
    private FilteredList<FindingModel> filteredFindings;

    @FXML
    public void initialize() {
        setupTable();
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

    private void setupTable() {
        colSeverity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity().getDisplayName()));
        colSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    FindingModel fm = getTableRow().getItem();
                    setGraphic(UiUtils.createSeverityBadge(fm.getSeverity()));
                }
            }
        });

        colScore.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f", d.getValue().getScore())));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    FindingModel fm = getTableRow().getItem();
                    Label scoreLabel = new Label(String.format("%.2f", fm.getScore()));
                    String color = fm.getScore() >= 0.9 ? "#EF4444" : (fm.getScore() >= 0.75 ? "#F97316" : "#EAB308");
                    scoreLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                    setGraphic(scoreLabel);
                }
            }
        });

        colEventType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventType()));
        colReason.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason()));
        colMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetectionMethod()));
        colEventId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventId()));
        colTimestamp.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.formatShortDate(d.getValue().getTimestamp())));

        anomaliesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showDetails(newVal);
        });

        filteredFindings = new FilteredList<>(masterFindings, p -> true);
        SortedList<FindingModel> sorted = new SortedList<>(filteredFindings);
        sorted.comparatorProperty().bind(anomaliesTable.comparatorProperty());
        anomaliesTable.setItems(sorted);
    }

    private void setupFilters() {
        severityFilterComboBox.getItems().setAll("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW");
        severityFilterComboBox.setValue("ALL");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        severityFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    private void updatePredicate() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String sevFilter = severityFilterComboBox.getValue();

        filteredFindings.setPredicate(f -> {
            boolean matchesSev = sevFilter == null || "ALL".equalsIgnoreCase(sevFilter) ||
                    (f.getSeverity() != null && f.getSeverity().name().equalsIgnoreCase(sevFilter));

            if (!matchesSev) return false;
            if (query.isEmpty()) return true;

            return (f.getReason() != null && f.getReason().toLowerCase().contains(query)) ||
                    (f.getEventType() != null && f.getEventType().toLowerCase().contains(query)) ||
                    (f.getDetectionMethod() != null && f.getDetectionMethod().toLowerCase().contains(query)) ||
                    (f.getEventId() != null && f.getEventId().toLowerCase().contains(query)) ||
                    (f.getAffectedEntity() != null && f.getAffectedEntity().toLowerCase().contains(query));
        });

        anomalyCountLabel.setText("Showing " + filteredFindings.size() + " of " + masterFindings.size() + " anomalies");
    }

    @FXML
    @Override
    public void refreshData() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        boolean isMock = AppConfig.getInstance().isMockModeEnabled();

        if (mockNoticeLabel != null) {
            mockNoticeLabel.setVisible(isMock);
            mockNoticeLabel.setManaged(isMock);
        }

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
            masterFindings.clear();
            showDetails(null);
            return;
        }

        final CaseModel currentCase = activeCase;
        activeCaseHeaderLabel.setText("ANOMALY INTELLIGENCE FOR " + currentCase.getCaseNumber() + " : " + currentCase.getTitle());

        showLoading("Querying AI analysis results (GET /api/cases/" + currentCase.getId() + "/analysis)...");

        Task<AnalysisResultModel> task = new Task<>() {
            @Override
            protected AnalysisResultModel call() throws Exception {
                AnalysisService service = ServiceFactory.getInstance().getAnalysisService();
                return service.getAnalysisByCase(currentCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            AnalysisResultModel result = task.getValue();
            if (result != null && result.getFindings() != null) {
                masterFindings.setAll(result.getFindings());
            } else {
                masterFindings.clear();
            }

            anomalyCountLabel.setText("Showing " + masterFindings.size() + " anomalies");

            if (!masterFindings.isEmpty()) {
                anomaliesTable.getSelectionModel().selectFirst();
            } else {
                showDetails(null);
            }
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch Analysis", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void showDetails(FindingModel fm) {
        if (fm == null) {
            detailSeverityBadge.getChildren().clear();
            detailScore.setText("0.00");
            detailScoreBar.setProgress(0.0);
            detailEventType.setText("No Anomaly Selected");
            detailTimestamp.setText("N/A");
            detailEventId.setText("N/A");
            detailMethod.setText("N/A");
            detailAffectedEntity.setText("N/A");
            detailReasonArea.setText("Select an anomaly finding from the table to inspect analytical details.");
            detailSuggestedActionArea.setText("");
            return;
        }

        detailSeverityBadge.getChildren().setAll(UiUtils.createSeverityBadge(fm.getSeverity()));
        detailScore.setText(String.format("%.2f / 1.00", fm.getScore()));
        detailScoreBar.setProgress(fm.getScore());

        String barColor = fm.getScore() >= 0.9 ? "#EF4444" : (fm.getScore() >= 0.75 ? "#F97316" : "#EAB308");
        detailScoreBar.setStyle("-fx-accent: " + barColor + ";");

        detailEventType.setText(fm.getEventType());
        detailTimestamp.setText(DateTimeUtils.formatUtc(fm.getTimestamp()));
        detailEventId.setText(fm.getEventId() != null ? fm.getEventId() : "N/A");
        detailMethod.setText(fm.getDetectionMethod() != null ? fm.getDetectionMethod() : "AI Heuristic");
        detailAffectedEntity.setText(fm.getAffectedEntity() != null ? fm.getAffectedEntity() : "Unspecified");
        detailReasonArea.setText(fm.getReason() != null ? fm.getReason() : "");
        detailSuggestedActionArea.setText(fm.getSuggestedAction() != null ? fm.getSuggestedAction() : "Follow standard forensic triage procedure.");
    }
}
