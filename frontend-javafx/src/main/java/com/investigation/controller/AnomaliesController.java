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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Controller for AI Anomaly Findings matching the reference design:
 * - Donut / Pie distribution chart with center total
 * - Anomaly trend line chart
 * - Table of suspicious clusters and detection findings
 */
public class AnomaliesController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> severityFilterComboBox;
    @FXML private Label anomalyCountLabel;

    // Charts
    @FXML private PieChart distributionPieChart;
    @FXML private Label donutTotalLabel;
    @FXML private Label legendCritical;
    @FXML private Label legendHigh;
    @FXML private Label legendMedium;
    @FXML private Label legendLow;

    @FXML private LineChart<String, Number> trendLineChart;
    @FXML private CategoryAxis trendXAxis;
    @FXML private NumberAxis trendYAxis;

    @FXML private TableView<FindingModel> anomaliesTable;
    @FXML private TableColumn<FindingModel, String> colSeverity;
    @FXML private TableColumn<FindingModel, String> colScore;
    @FXML private TableColumn<FindingModel, String> colEventType;
    @FXML private TableColumn<FindingModel, String> colReason;
    @FXML private TableColumn<FindingModel, String> colMethod;
    @FXML private TableColumn<FindingModel, String> colEventId;
    @FXML private TableColumn<FindingModel, String> colTimestamp;

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
        Label emptyLbl = new Label("0 anomalies detected in the analyzed evidence.\nAll observed events conform to standard operational baseline.");
        emptyLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-alignment: center; -fx-text-alignment: center;");
        anomaliesTable.setPlaceholder(emptyLbl);

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
        colEventType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventType()));
        colReason.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason()));
        colMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetectionMethod()));
        colEventId.setCellValueFactory(d -> {
            String id = d.getValue().getEventId();
            if (id != null && id.contains(":")) {
                id = id.substring(id.lastIndexOf(":") + 1);
            }
            if (id != null && id.length() > 8) {
                id = id.substring(0, 8);
            }
            return new SimpleStringProperty(id != null ? id : "N/A");
        });
        colTimestamp.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.formatShortDate(d.getValue().getTimestamp())));

        filteredFindings = new FilteredList<>(masterFindings, f -> true);
        anomaliesTable.setItems(filteredFindings);
    }

    private void setupFilters() {
        severityFilterComboBox.getItems().setAll("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW");
        severityFilterComboBox.setValue("ALL");

        searchField.textProperty().addListener((obs, o, n) -> updateFilter());
        severityFilterComboBox.valueProperty().addListener((obs, o, n) -> updateFilter());
    }

    private void updateFilter() {
        String q = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String sev = severityFilterComboBox.getValue();

        filteredFindings.setPredicate(f -> {
            boolean matchesSev = sev == null || "ALL".equalsIgnoreCase(sev) ||
                    (f.getSeverity() != null && f.getSeverity().name().equalsIgnoreCase(sev));
            if (!matchesSev) return false;
            if (q.isEmpty()) return true;

            return (f.getReason() != null && f.getReason().toLowerCase().contains(q)) ||
                    (f.getEventType() != null && f.getEventType().toLowerCase().contains(q)) ||
                    (f.getDetectionMethod() != null && f.getDetectionMethod().toLowerCase().contains(q));
        });

        anomalyCountLabel.setText("Showing " + filteredFindings.size() + " anomalies");
    }

    @FXML
    @Override
    public void refreshData() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            activeCaseHeaderLabel.setText("[No Case Selected]");
            masterFindings.clear();
            renderCharts(0, 0, 0, 0);
            return;
        }

        activeCaseHeaderLabel.setText("[" + activeCase.getCaseNumber() + " - " + activeCase.getTitle() + "]");
        showLoading("Loading anomaly detections for " + activeCase.getCaseNumber() + "...");

        Task<AnalysisResultModel> task = new Task<>() {
            @Override
            protected AnalysisResultModel call() throws Exception {
                AnalysisService service = ServiceFactory.getInstance().getAnalysisService();
                return service.getAnalysisByCase(activeCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            AnalysisResultModel arm = task.getValue();
            if (arm != null && arm.getFindings() != null) {
                masterFindings.setAll(arm.getFindings());
                int crit = 0, high = 0, med = 0, low = 0;
                for (FindingModel fm : arm.getFindings()) {
                    if (fm.getSeverity() == Severity.CRITICAL) crit++;
                    else if (fm.getSeverity() == Severity.HIGH) high++;
                    else if (fm.getSeverity() == Severity.MEDIUM) med++;
                    else low++;
                }
                renderCharts(crit, high, med, low);
            } else {
                masterFindings.clear();
                renderCharts(0, 0, 0, 0);
            }
            anomalyCountLabel.setText("Showing " + masterFindings.size() + " anomalies");
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch Anomalies", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void renderCharts(int crit, int high, int med, int low) {
        int total = crit + high + med + low;
        if (donutTotalLabel != null) {
            donutTotalLabel.setText(String.valueOf(total));
        }

        // Donut Chart: reflect real detected breakdown
        if (distributionPieChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            if (crit > 0) pieData.add(new PieChart.Data("Critical", crit));
            if (high > 0) pieData.add(new PieChart.Data("High", high));
            if (med > 0) pieData.add(new PieChart.Data("Medium", med));
            if (low > 0) pieData.add(new PieChart.Data("Low", low));
            if (total == 0) {
                pieData.add(new PieChart.Data("Normal Baseline", 1));
            }
            distributionPieChart.setData(pieData);
        }

        if (legendCritical != null) legendCritical.setText("Critical " + crit);
        if (legendHigh != null) legendHigh.setText("High " + high);
        if (legendMedium != null) legendMedium.setText("Medium " + med);
        if (legendLow != null) legendLow.setText("Low " + low);

        // Trend Line Chart: reflect real findings temporal distribution
        if (trendLineChart != null) {
            trendLineChart.getData().clear();
            if (!masterFindings.isEmpty()) {
                java.util.Map<String, Integer> countsByDate = new java.util.LinkedHashMap<>();
                for (FindingModel fm : masterFindings) {
                    String dateKey = fm.getTimestamp() != null
                            ? DateTimeUtils.formatShortDate(fm.getTimestamp())
                            : "Analyzed";
                    countsByDate.put(dateKey, countsByDate.getOrDefault(dateKey, 0) + 1);
                }
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Detected Anomalies");
                countsByDate.forEach((date, count) -> series.getData().add(new XYChart.Data<>(date, count)));
                trendLineChart.getData().add(series);
            }
        }
    }
}
