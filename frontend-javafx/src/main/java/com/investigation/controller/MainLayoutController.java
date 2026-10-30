package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.service.CaseService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the main application shell containing:
 * - Compact top header with brand, clock, mock toggle, active case selector
 * - Fixed left sidebar with exactly 7 navigation items
 * - Center stack pane hosting views
 */
public class MainLayoutController {

    @FXML private BorderPane rootBorderPane;
    @FXML private Label utcClockLabel;
    @FXML private Label mockBadge;
    @FXML private Label backendStatusLabel;
    @FXML private ComboBox<CaseModel> activeCaseComboBox;
    @FXML private StackPane contentArea;

    // Sidebar navigation buttons (in exact requested order)
    @FXML private Button navDashboard;
    @FXML private Button navCases;
    @FXML private Button navEvidence;
    @FXML private Button navTimeline;
    @FXML private Button navGraph;
    @FXML private Button navAnomalies;
    @FXML private Button navAiSummary;

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private final List<Button> navButtons = new ArrayList<>();

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'");

    @FXML
    public void initialize() {
        navButtons.addAll(List.of(
                navDashboard, navCases, navEvidence, navTimeline, navGraph, navAnomalies, navAiSummary
        ));

        setupClock();
        setupConfigBindings();

        // Immediately set initial active case from available cases synchronously
        try {
            List<CaseModel> initialCases = ServiceFactory.getInstance().getCaseService().getAllCases();
            if (!initialCases.isEmpty()) {
                activeCaseComboBox.getItems().setAll(initialCases);
                if (AppConfig.getInstance().getActiveCase() == null) {
                    AppConfig.getInstance().setActiveCase(initialCases.get(0));
                    activeCaseComboBox.setValue(initialCases.get(0));
                }
            }
        } catch (Exception ignored) {}

        loadCasesForSwitcher();

        // Default to Dashboard
        navigateTo("Dashboard", "/com/investigation/view/DashboardView.fxml", navDashboard);
    }

    private void setupClock() {
        Thread clockThread = new Thread(() -> {
            while (true) {
                try {
                    String timeStr = ZonedDateTime.now(ZoneOffset.UTC).format(CLOCK_FORMAT);
                    Platform.runLater(() -> {
                        if (utcClockLabel != null) {
                            utcClockLabel.setText(timeStr);
                        }
                    });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();
    }

    private void setupConfigBindings() {
        AppConfig config = AppConfig.getInstance();

        // Mock badge update
        config.mockModeEnabledProperty().addListener((obs, oldVal, isMock) -> {
            updateStatusBadges(isMock, config.isBackendConnected());
        });

        config.backendConnectedProperty().addListener((obs, oldVal, isConn) -> {
            updateStatusBadges(config.isMockModeEnabled(), isConn);
        });

        updateStatusBadges(config.isMockModeEnabled(), config.isBackendConnected());

        // Active Case selector
        activeCaseComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CaseModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCaseNumber() + " - " + item.getTitle());
            }
        });
        activeCaseComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CaseModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Active Case" : item.getCaseNumber() + " - " + item.getTitle());
            }
        });

        activeCaseComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldCase, newCase) -> {
            if (newCase != null) {
                config.setActiveCase(newCase);
                refreshCurrentView();
            }
        });

        config.activeCaseProperty().addListener((obs, oldCase, newCase) -> {
            if (newCase != null && !Objects.equals(activeCaseComboBox.getValue(), newCase)) {
                activeCaseComboBox.setValue(newCase);
            }
        });
    }

    private void updateStatusBadges(boolean isMock, boolean isConnected) {
        Platform.runLater(() -> {
            if (isMock) {
                mockBadge.setText("SIMULATED MOCK MODE");
                mockBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-border-color: #F59E0B; " +
                        "-fx-text-fill: #F59E0B; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else {
                mockBadge.setText("LIVE REST API MODE");
                mockBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-border-color: #10B981; " +
                        "-fx-text-fill: #10B981; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
            }

            if (isConnected) {
                backendStatusLabel.setText("BACKEND: CONNECTED");
                backendStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else {
                backendStatusLabel.setText("BACKEND: OFFLINE (PORT 8080)");
                backendStatusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
        });
    }

    public void loadCasesForSwitcher() {
        Task<List<CaseModel>> task = new Task<>() {
            @Override
            protected List<CaseModel> call() throws Exception {
                CaseService cs = ServiceFactory.getInstance().getCaseService();
                return cs.getAllCases();
            }
        };

        task.setOnSucceeded(e -> {
            List<CaseModel> list = task.getValue();
            activeCaseComboBox.getItems().setAll(list);
            if (!list.isEmpty() && AppConfig.getInstance().getActiveCase() == null) {
                activeCaseComboBox.setValue(list.get(0));
            } else if (AppConfig.getInstance().getActiveCase() != null) {
                activeCaseComboBox.setValue(AppConfig.getInstance().getActiveCase());
            }
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    // Sidebar navigation action handlers (in exact requested order)
    @FXML private void onNavDashboard() { navigateTo("Dashboard", "/com/investigation/view/DashboardView.fxml", navDashboard); }
    @FXML private void onNavCases() { navigateTo("Cases", "/com/investigation/view/CasesView.fxml", navCases); }
    @FXML private void onNavEvidence() { navigateTo("Evidence", "/com/investigation/view/EvidenceView.fxml", navEvidence); }
    @FXML private void onNavTimeline() { navigateTo("Timeline", "/com/investigation/view/TimelineView.fxml", navTimeline); }
    @FXML private void onNavGraph() { navigateTo("RelationshipGraph", "/com/investigation/view/GraphView.fxml", navGraph); }
    @FXML private void onNavAnomalies() { navigateTo("Anomalies", "/com/investigation/view/AnomaliesView.fxml", navAnomalies); }
    @FXML private void onNavAiSummary() { navigateTo("AiSummary", "/com/investigation/view/AiSummaryView.fxml", navAiSummary); }

    @FXML
    private void onToggleMockMode() {
        AppConfig config = AppConfig.getInstance();
        boolean newMode = !config.isMockModeEnabled();
        config.setMockModeEnabled(newMode);

        if (!newMode) {
            // Check if backend is alive
            Task<Boolean> testTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return ServiceFactory.getInstance().getApiClient().testConnection();
                }
            };
            testTask.setOnSucceeded(e -> {
                boolean alive = testTask.getValue();
                if (!alive) {
                    UiUtils.showError("Backend Connection Warning",
                            "Could not reach Spring Boot backend at " + config.getBackendBaseUrl() +
                                    ". Requests may fail until the backend server is running.\n\nYou can toggle back to Mock Mode anytime.");
                }
                loadCasesForSwitcher();
                refreshCurrentView();
            });
            new Thread(testTask).start();
        } else {
            loadCasesForSwitcher();
            refreshCurrentView();
        }
    }

    private String currentViewKey = null;

    private void navigateTo(String key, String fxmlPath, Button activeBtn) {
        currentViewKey = key;
        for (Button b : navButtons) {
            b.getStyleClass().remove("nav-button-active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }

        try {
            Node viewNode;
            Object controller;

            if (viewCache.containsKey(key)) {
                viewNode = viewCache.get(key);
                controller = controllerCache.get(key);
                if (controller instanceof Refreshable) {
                    ((Refreshable) controller).refreshData();
                }
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                viewNode = loader.load();
                controller = loader.getController();

                viewCache.put(key, viewNode);
                controllerCache.put(key, controller);
            }

            contentArea.getChildren().setAll(viewNode);

        } catch (IOException e) {
            e.printStackTrace();
            contentArea.getChildren().setAll(UiUtils.createEmptyState("⚠️", "Failed to Load Screen: " + key, e.getMessage()));
        }
    }

    public void refreshCurrentView() {
        if (currentViewKey != null && controllerCache.containsKey(currentViewKey)) {
            Object ctrl = controllerCache.get(currentViewKey);
            if (ctrl instanceof Refreshable) {
                ((Refreshable) ctrl).refreshData();
            }
        }
    }

    public interface Refreshable {
        void refreshData();
    }
}
