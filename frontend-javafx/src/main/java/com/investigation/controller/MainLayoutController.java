package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.UserSession;
import com.investigation.service.CaseService;
import com.investigation.service.ServiceFactory;
import com.investigation.service.SessionService;
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
import java.util.*;

/**
 * Controller for the main application shell containing:
 * - Professional dark navy top bar with shield crest brand, global search, case switcher, user profile
 * - Dark navy left sidebar with 8 primary investigation modules matching reference design
 * - Crisp light workspace container
 */
public class MainLayoutController {

    private static MainLayoutController instance;

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML private BorderPane rootBorderPane;
    @FXML private TextField globalSearchField;
    @FXML private Label backendStatusLabel;
    @FXML private ComboBox<CaseModel> activeCaseComboBox;
    @FXML private StackPane contentArea;
    @FXML private Label userCallsignLabel;
    @FXML private Button logoutButton;

    // Sidebar navigation buttons
    @FXML private Button navDashboard;
    @FXML private Button navCases;
    @FXML private Button navEvidence;
    @FXML private Button navTimeline;
    @FXML private Button navGraph;
    @FXML private Button navAnomalies;
    @FXML private Button navAiSummary;

    // Admin module section (restricted)
    @FXML private javafx.scene.layout.VBox adminNavSection;
    @FXML private Button navAdmin;

    private final Map<String, Node> viewCache = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private final List<Button> navButtons = new ArrayList<>();

    @FXML
    public void initialize() {
        instance = this;

        navButtons.addAll(List.of(
                navDashboard, navCases, navEvidence, navTimeline, navGraph, navAnomalies, navAiSummary
        ));
        if (navAdmin != null) {
            navButtons.add(navAdmin);
        }

        setupConfigBindings();
        updateUserBadge();

        SessionService.getInstance().currentSessionProperty().addListener((obs, oldVal, newVal) -> {
            updateUserBadge();
        });

        // Perform initial non-blocking backend connectivity probe
        Task<Boolean> connTask = new Task<>() {
            @Override
            protected Boolean call() {
                return ServiceFactory.getInstance().getApiClient().testConnection();
            }
        };
        connTask.setOnSucceeded(e -> {
            boolean connected = connTask.getValue();
            AppConfig.getInstance().setBackendConnected(connected);
            if (backendStatusLabel != null) {
                backendStatusLabel.setText(connected ? "SYSTEM READY" : "STANDALONE MODE");
                backendStatusLabel.setStyle(connected ? "-fx-text-fill: #10B981; -fx-font-size: 10px; -fx-font-weight: bold;" : "-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            loadCasesForSwitcher();
        });
        connTask.setOnFailed(e -> {
            AppConfig.getInstance().setBackendConnected(false);
            if (backendStatusLabel != null) {
                backendStatusLabel.setText("STANDALONE MODE");
                backendStatusLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            loadCasesForSwitcher();
        });
        Thread connThread = new Thread(connTask);
        connThread.setDaemon(true);
        connThread.start();

        // Setup global search listener
        if (globalSearchField != null) {
            globalSearchField.setOnAction(e -> {
                String query = globalSearchField.getText();
                if (query != null && !query.isBlank()) {
                    navigateTo("Cases", "/com/investigation/view/CasesView.fxml", navCases);
                    Object ctrl = controllerCache.get("Cases");
                    if (ctrl instanceof CasesController cc) {
                        cc.applySearchFilter(query);
                    }
                }
            });
        }

        // Default to Dashboard
        navigateTo("Dashboard", "/com/investigation/view/DashboardView.fxml", navDashboard);
    }

    private void setupConfigBindings() {
        AppConfig config = AppConfig.getInstance();

        // Active Case selector styling & binding
        activeCaseComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CaseModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getCaseNumber() + " - " + item.getTitle());
                    setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px;");
                }
            }
        });
        activeCaseComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CaseModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select Case...");
                    setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold;");
                } else {
                    setText(item.getCaseNumber() + " - " + item.getTitle());
                    setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold;");
                }
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

    // Sidebar navigation action handlers
    @FXML public void onNavDashboard() { navigateTo("Dashboard", "/com/investigation/view/DashboardView.fxml", navDashboard); }
    @FXML public void onNavCases() { navigateTo("Cases", "/com/investigation/view/CasesView.fxml", navCases); }
    @FXML public void onNavEvidence() { navigateTo("Evidence", "/com/investigation/view/EvidenceView.fxml", navEvidence); }
    @FXML public void onNavTimeline() { navigateTo("Timeline", "/com/investigation/view/TimelineView.fxml", navTimeline); }
    @FXML public void onNavGraph() { navigateTo("RelationshipGraph", "/com/investigation/view/GraphView.fxml", navGraph); }
    @FXML public void onNavAnomalies() { navigateTo("Anomalies", "/com/investigation/view/AnomaliesView.fxml", navAnomalies); }
    @FXML public void onNavAiSummary() { navigateTo("AiSummary", "/com/investigation/view/AiSummaryView.fxml", navAiSummary); }
    @FXML public void onNavAdmin() { navigateTo("Admin", "/com/investigation/view/AdminView.fxml", navAdmin); }

    public void navigateToScreen(String screenName) {
        switch (screenName) {
            case "Dashboard" -> onNavDashboard();
            case "Cases" -> onNavCases();
            case "Evidence" -> onNavEvidence();
            case "Timeline" -> onNavTimeline();
            case "RelationshipGraph", "Graph" -> onNavGraph();
            case "Anomalies", "Findings" -> onNavAnomalies();
            case "AiSummary", "Summary" -> onNavAiSummary();
            case "Admin" -> onNavAdmin();
            default -> onNavDashboard();
        }
    }

    @FXML
    private void onLogout() {
        SessionService.getInstance().logout(ServiceFactory.getInstance().getApiClient());
    }

    private void updateUserBadge() {
        Platform.runLater(() -> {
            if (userCallsignLabel == null) return;
            UserSession session = SessionService.getInstance().getCurrentSession();
            boolean isAdmin = session != null && session.isAdmin();

            if (adminNavSection != null) {
                adminNavSection.setVisible(isAdmin);
                adminNavSection.setManaged(isAdmin);
            }

            if (session != null && session.getUsername() != null && !session.getUsername().isBlank()) {
                String role = session.getRole() != null ? session.getRole() : "INVESTIGATOR";
                userCallsignLabel.setText(session.getUsername() + " (" + role + ")");
            } else {
                userCallsignLabel.setText("Investigator");
            }

            if (!isAdmin && "Admin".equals(currentViewKey)) {
                navigateTo("Dashboard", "/com/investigation/view/DashboardView.fxml", navDashboard);
            }
        });
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
