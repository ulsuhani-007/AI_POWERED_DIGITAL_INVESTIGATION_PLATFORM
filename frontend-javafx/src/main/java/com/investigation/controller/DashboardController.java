package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.DashboardStats;
import com.investigation.service.AnalysisService;
import com.investigation.service.CaseService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class DashboardController implements MainLayoutController.Refreshable {

    @FXML private Label totalCasesLabel;
    @FXML private Label totalEvidenceLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalAnomaliesLabel;
    @FXML private Label openCasesLabel;
    @FXML private Label criticalAnomaliesLabel;
    @FXML private Label mockNoticeLabel;

    @FXML private TableView<CaseModel> casesSummaryTable;
    @FXML private TableColumn<CaseModel, String> colCaseNumber;
    @FXML private TableColumn<CaseModel, String> colTitle;
    @FXML private TableColumn<CaseModel, String> colPriority;
    @FXML private TableColumn<CaseModel, String> colStatus;
    @FXML private TableColumn<CaseModel, String> colEvents;
    @FXML private TableColumn<CaseModel, String> colAnomalies;

    @FXML private VBox recentActivityContainer;
    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;

    @FXML
    public void initialize() {
        setupTableColumns();
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

    private void setupTableColumns() {
        colCaseNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCaseNumber()));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        colPriority.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPriority().getDisplayName()));
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CaseModel cm = getTableRow().getItem();
                    setGraphic(UiUtils.createPriorityBadge(cm.getPriority()));
                }
            }
        });

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CaseModel cm = getTableRow().getItem();
                    setGraphic(UiUtils.createStatusBadge(cm.getStatus()));
                }
            }
        });

        colEvents.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEventCount())));
        colAnomalies.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAnomalyCount())));

        casesSummaryTable.setRowFactory(tv -> {
            TableRow<CaseModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    CaseModel clicked = row.getItem();
                    AppConfig.getInstance().setActiveCase(clicked);
                }
            });
            return row;
        });
    }

    @FXML
    @Override
    public void refreshData() {
        boolean isMock = AppConfig.getInstance().isMockModeEnabled();
        if (mockNoticeLabel != null) {
            mockNoticeLabel.setVisible(isMock);
            mockNoticeLabel.setManaged(isMock);
        }

        showLoading("Refreshing Dashboard Intelligence...");

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                AnalysisService analysisService = ServiceFactory.getInstance().getAnalysisService();
                CaseService caseService = ServiceFactory.getInstance().getCaseService();

                DashboardStats stats = analysisService.getDashboardStats();
                List<CaseModel> cases = caseService.getAllCases();
                return new DashboardData(stats, cases);
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            DashboardData data = task.getValue();
            populateData(data.stats, data.cases);
        });

        task.setOnFailed(e -> {
            hideLoading();
            Throwable ex = task.getException();
            UiUtils.showError("Failed to Load Dashboard", ex != null ? ex.getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void populateData(DashboardStats stats, List<CaseModel> cases) {
        if (stats != null) {
            totalCasesLabel.setText(String.valueOf(stats.getTotalCases()));
            totalEvidenceLabel.setText(String.valueOf(stats.getTotalEvidenceRecords()));
            totalEventsLabel.setText(String.format("%,d", stats.getTotalProcessedEvents()));
            totalAnomaliesLabel.setText(String.valueOf(stats.getTotalAnomalies()));
            openCasesLabel.setText(stats.getOpenCasesCount() + " Active");
            criticalAnomaliesLabel.setText(stats.getCriticalAnomaliesCount() + " Critical");

            // Activity feed
            recentActivityContainer.getChildren().clear();
            if (stats.getRecentActivity() != null && !stats.getRecentActivity().isEmpty()) {
                for (DashboardStats.ActivityItem item : stats.getRecentActivity()) {
                    recentActivityContainer.getChildren().add(createActivityItemNode(item));
                }
            } else {
                recentActivityContainer.getChildren().add(new Label("No recent activity logged."));
            }
        }

        if (cases != null) {
            casesSummaryTable.getItems().setAll(cases);
        }
    }

    private VBox createActivityItemNode(DashboardStats.ActivityItem item) {
        VBox card = new VBox(3);
        card.setStyle("-fx-background-color: #0F172A; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: #1E293B; -fx-border-radius: 6px;");

        HBox header = new HBox(8);
        Label title = new Label(item.getTitle());
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        Label badge = new Label(item.getType());
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-padding: 2px 6px; -fx-background-radius: 4px;");

        header.getChildren().addAll(badge, title);

        Label desc = new Label(item.getDescription());
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        desc.setWrapText(true);

        HBox footer = new HBox(12);
        Label time = new Label(DateTimeUtils.getRelativeTime(item.getTimestamp()));
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");

        Label caseNum = new Label("Ref: " + item.getCaseNumber());
        caseNum.setStyle("-fx-font-size: 10px; -fx-text-fill: #00F0FF;");

        footer.getChildren().addAll(time, caseNum);

        card.getChildren().addAll(header, desc, footer);
        return card;
    }

    private static class DashboardData {
        final DashboardStats stats;
        final List<CaseModel> cases;

        DashboardData(DashboardStats stats, List<CaseModel> cases) {
            this.stats = stats;
            this.cases = cases;
        }
    }
}
