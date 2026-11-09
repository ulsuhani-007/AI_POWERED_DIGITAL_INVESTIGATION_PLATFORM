package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;
import com.investigation.service.CaseService;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Cases Management and Case Details view matching the reference design:
 * - Table view with realistic thumbnails, priority & status badges, and action menus
 * - Seamless Case Details view with milestone stepper, jump-tabs, and contextual metrics
 * - Proper JavaFX event handling (listeners, callbacks, sorting, filtering)
 */
public class CasesController implements MainLayoutController.Refreshable {

    @FXML private VBox tableViewContainer;
    @FXML private VBox caseDetailsContainer;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private MenuButton sortMenuButton;

    @FXML private TableView<CaseModel> casesTable;
    @FXML private TableColumn<CaseModel, Void> colThumb;
    @FXML private TableColumn<CaseModel, String> colCaseNum;
    @FXML private TableColumn<CaseModel, String> colTitle;
    @FXML private TableColumn<CaseModel, String> colStatus;
    @FXML private TableColumn<CaseModel, String> colPriority;
    @FXML private TableColumn<CaseModel, String> colInvestigator;
    @FXML private TableColumn<CaseModel, String> colEvidence;
    @FXML private TableColumn<CaseModel, String> colEvents;
    @FXML private TableColumn<CaseModel, String> colAnomalies;
    @FXML private TableColumn<CaseModel, String> colCreated;
    @FXML private TableColumn<CaseModel, Void> colActions;

    // Case Details UI Elements (Panel 4)
    @FXML private StackPane detailThumbnailContainer;
    @FXML private Label detailCaseHeaderTitle;
    @FXML private Label detailCaseDescription;
    @FXML private HBox detailHeaderBadges;
    @FXML private Label detailInvestigatorLabel;
    @FXML private Label detailTotalEvidence;
    @FXML private Label detailTotalEvents;
    @FXML private Label detailTotalAnomalies;
    @FXML private Label detailProgressPercent;
    @FXML private ProgressBar detailProgressBar;
    @FXML private Label detailPriorityText;
    @FXML private Label detailStatusText;
    @FXML private Label detailUpdatedText;
    @FXML private Button setActiveBtn;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;

    private final ObservableList<CaseModel> masterData = FXCollections.observableArrayList();
    private FilteredList<CaseModel> filteredData;
    private CaseModel selectedCase = null;

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
        // Thumbnail column
        colThumb.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CaseModel cm = getTableRow().getItem();
                    setGraphic(UiUtils.createCaseThumbnailView(cm.getCaseNumber(), cm.getTitle(), 48, 32, 4));
                }
            }
        });

        colCaseNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCaseNumber()));
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CaseModel cm = getTableRow().getItem();
                    if (UiUtils.isHistoricalCase(cm.getCaseNumber())) {
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER_LEFT);
                        Label titleLbl = new Label(cm.getTitle());
                        titleLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #0F172A;");
                        Label demoLbl = new Label("HISTORICAL DEMO");
                        demoLbl.setStyle("-fx-background-color: #EEF2FF; -fx-text-fill: #4338CA; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
                        box.getChildren().addAll(titleLbl, demoLbl);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setText(item);
                        setStyle("-fx-font-weight: 600; -fx-text-fill: #0F172A;");
                        setGraphic(null);
                    }
                }
            }
        });

        // Status badge
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiUtils.createStatusBadge(getTableRow().getItem().getStatus()));
                }
            }
        });

        // Priority badge
        colPriority.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPriority().getDisplayName()));
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UiUtils.createPriorityBadge(getTableRow().getItem().getPriority()));
                }
            }
        });

        colInvestigator.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getLeadInvestigator() != null ? d.getValue().getLeadInvestigator() : "Unassigned"
        ));

        colEvidence.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEvidenceCount())));
        colEvents.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEventCount())));
        colAnomalies.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAnomalyCount())));

        colCreated.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUpdatedAt() != null ? DateTimeUtils.getRelativeTime(d.getValue().getUpdatedAt()) : DateTimeUtils.formatShortDate(d.getValue().getCreatedAt())
        ));

        // Actions column with Open and Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            private final Button delBtn = new Button("🗑️");
            private final HBox btnBox = new HBox(6.0, openBtn, delBtn);
            {
                btnBox.setAlignment(Pos.CENTER);
                openBtn.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-border-color: #BFDBFE; -fx-border-radius: 4px; -fx-cursor: hand;");
                delBtn.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-border-color: #FECACA; -fx-border-radius: 4px; -fx-cursor: hand;");
                delBtn.setTooltip(new Tooltip("Permanently Delete Case"));
                openBtn.setTooltip(new Tooltip("Open Case Details"));

                openBtn.setOnAction(e -> {
                    CaseModel cm = getTableRow().getItem();
                    if (cm != null) {
                        openCaseDetails(cm);
                    }
                });

                delBtn.setOnAction(e -> {
                    CaseModel cm = getTableRow().getItem();
                    if (cm != null) {
                        promptDeleteCase(cm);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(btnBox);
                }
            }
        });

        // Double click row opens details, right-click context menu offers Open / Delete
        casesTable.setRowFactory(tv -> {
            TableRow<CaseModel> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();
            MenuItem openItem = new MenuItem("Open Case Details");
            openItem.setOnAction(e -> {
                if (!row.isEmpty()) openCaseDetails(row.getItem());
            });
            MenuItem deleteItem = new MenuItem("Delete Case");
            deleteItem.setStyle("-fx-text-fill: #DC2626;");
            deleteItem.setOnAction(e -> {
                if (!row.isEmpty()) promptDeleteCase(row.getItem());
            });
            contextMenu.getItems().addAll(openItem, new SeparatorMenuItem(), deleteItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openCaseDetails(row.getItem());
                }
            });
            return row;
        });

        casesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.selectedCase = newVal;
            }
        });

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<CaseModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(casesTable.comparatorProperty());
        casesTable.setItems(sortedData);
    }

    private void setupFilters() {
        statusFilterComboBox.getItems().setAll("ALL", "OPEN", "IN_PROGRESS", "CLOSED", "ARCHIVED");
        statusFilterComboBox.setValue("ALL");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    public void applySearchFilter(String query) {
        if (searchField != null) {
            searchField.setText(query);
        }
    }

    private void updatePredicate() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String statusFilter = statusFilterComboBox.getValue();

        filteredData.setPredicate(c -> {
            boolean matchesStatus = "ALL".equalsIgnoreCase(statusFilter) ||
                    (c.getStatus() != null && c.getStatus().name().equalsIgnoreCase(statusFilter));

            if (!matchesStatus) return false;

            if (query.isEmpty()) return true;

            return (c.getCaseNumber() != null && c.getCaseNumber().toLowerCase().contains(query)) ||
                    (c.getTitle() != null && c.getTitle().toLowerCase().contains(query)) ||
                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(query)) ||
                    (c.getLeadInvestigator() != null && c.getLeadInvestigator().toLowerCase().contains(query));
        });
    }

    // Sorting actions
    @FXML private void onSortByNumber() { masterData.sort(Comparator.comparing(CaseModel::getCaseNumber)); }
    @FXML private void onSortByTitle() { masterData.sort(Comparator.comparing(CaseModel::getTitle)); }
    @FXML private void onSortByPriority() { masterData.sort(Comparator.comparing(CaseModel::getPriority)); }
    @FXML private void onSortByDate() {
        masterData.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
    }

    @FXML
    @Override
    public void refreshData() {
        showLoading("Loading Cases from repository...");

        Task<List<CaseModel>> task = new Task<>() {
            @Override
            protected List<CaseModel> call() throws Exception {
                CaseService service = ServiceFactory.getInstance().getCaseService();
                return service.getAllCases();
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            masterData.setAll(task.getValue());
            if (!masterData.isEmpty()) {
                CaseModel active = AppConfig.getInstance().getActiveCase();
                if (active != null && masterData.contains(active)) {
                    casesTable.getSelectionModel().select(active);
                    this.selectedCase = active;
                } else {
                    casesTable.getSelectionModel().selectFirst();
                    this.selectedCase = masterData.get(0);
                }
            }
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch Cases", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public void openCaseDetails(CaseModel cm) {
        if (cm == null) return;
        this.selectedCase = cm;
        AppConfig.getInstance().setActiveCase(cm);

        // Populate Case Details (Panel 4)
        detailCaseHeaderTitle.setText(cm.getCaseNumber() + " - " + cm.getTitle());
        detailCaseDescription.setText(cm.getDescription() != null && !cm.getDescription().isBlank()
                ? cm.getDescription() : "Investigation case record.");

        detailHeaderBadges.getChildren().clear();
        if (UiUtils.isHistoricalCase(cm.getCaseNumber())) {
            detailHeaderBadges.getChildren().add(UiUtils.createHistoricalBadge());
        }
        detailHeaderBadges.getChildren().addAll(
                UiUtils.createStatusBadge(cm.getStatus()),
                UiUtils.createPriorityBadge(cm.getPriority())
        );

        detailInvestigatorLabel.setText(cm.getLeadInvestigator() != null && !cm.getLeadInvestigator().isBlank()
                ? cm.getLeadInvestigator() : "Unassigned");

        detailTotalEvidence.setText(String.valueOf(cm.getEvidenceCount()));
        detailTotalEvents.setText(String.valueOf(cm.getEventCount()));
        detailTotalAnomalies.setText(String.valueOf(cm.getAnomalyCount()));

        int progress = cm.getStatus() == CaseStatus.CLOSED ? 100 :
                (cm.getAnomalyCount() > 0 ? 85 :
                (cm.getEventCount() > 0 ? 60 :
                (cm.getEvidenceCount() > 0 ? 35 : 15)));
        detailProgressPercent.setText(progress + "%");
        detailProgressBar.setProgress(progress / 100.0);

        detailPriorityText.setText(cm.getPriority().getDisplayName());
        detailPriorityText.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (cm.getPriority() == CasePriority.HIGH || cm.getPriority() == CasePriority.CRITICAL ? "#DC2626;" : "#D97706;"));

        detailStatusText.setText(cm.getStatus().getDisplayName());
        detailStatusText.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (cm.getStatus() == CaseStatus.CLOSED ? "#059669;" : "#D97706;"));

        detailUpdatedText.setText(cm.getUpdatedAt() != null ? DateTimeUtils.getRelativeTime(cm.getUpdatedAt()) : "Recently");

        // Banner thumbnail
        detailThumbnailContainer.getChildren().setAll(
                UiUtils.createCaseThumbnailView(cm.getCaseNumber(), cm.getTitle(), 120, 80, 8)
        );

        // Switch view
        tableViewContainer.setVisible(false);
        tableViewContainer.setManaged(false);
        caseDetailsContainer.setVisible(true);
        caseDetailsContainer.setManaged(true);
    }

    @FXML
    private void onCloseDetails() {
        caseDetailsContainer.setVisible(false);
        caseDetailsContainer.setManaged(false);
        tableViewContainer.setVisible(true);
        tableViewContainer.setManaged(true);
    }

    // Sub-navigation action tabs
    @FXML
    private void onJumpToEvidence() {
        if (selectedCase != null) AppConfig.getInstance().setActiveCase(selectedCase);
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("Evidence");
        }
    }

    @FXML
    private void onJumpToTimeline() {
        if (selectedCase != null) AppConfig.getInstance().setActiveCase(selectedCase);
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("Timeline");
        }
    }

    @FXML
    private void onJumpToGraph() {
        if (selectedCase != null) AppConfig.getInstance().setActiveCase(selectedCase);
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("RelationshipGraph");
        }
    }

    @FXML
    private void onJumpToAnomalies() {
        if (selectedCase != null) AppConfig.getInstance().setActiveCase(selectedCase);
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("Anomalies");
        }
    }

    @FXML
    private void onJumpToSummary() {
        if (selectedCase != null) AppConfig.getInstance().setActiveCase(selectedCase);
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("AiSummary");
        }
    }

    @FXML
    private void onSetActiveCase() {
        if (selectedCase != null) {
            AppConfig.getInstance().setActiveCase(selectedCase);
            UiUtils.showInfo("Active Focus Case Set", "Case " + selectedCase.getCaseNumber() + " is now selected across all analysis modules.");
        }
    }

    @FXML
    private void onNewCase() {
        Dialog<CaseModel> dialog = new Dialog<>();
        dialog.setTitle("Create New Investigation Case");
        dialog.setHeaderText("Register Investigation Case Record");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #CBD5E1;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        String generatedCaseNum = "CASE-" + (System.currentTimeMillis() % 1000000);
        TextField caseNumField = new TextField(generatedCaseNum);
        caseNumField.setPromptText("e.g. CASE-2026-001");

        TextField titleField = new TextField("Investigation " + generatedCaseNum);
        titleField.setPromptText("e.g. Corporate Securities Incident");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Case scope and investigative context...");
        descArea.setPrefRowCount(3);

        ComboBox<CasePriority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(CasePriority.values()));
        priorityBox.setValue(CasePriority.HIGH);

        String activeUser = com.investigation.service.SessionService.getInstance().getCurrentSession() != null
                ? com.investigation.service.SessionService.getInstance().getCurrentSession().getUsername() : "Investigator";
        TextField investigatorField = new TextField(activeUser);
        investigatorField.setPromptText("Lead Investigator");

        grid.add(new Label("Case Number: *"), 0, 0);
        grid.add(caseNumField, 1, 0);
        grid.add(new Label("Case Title: *"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Priority:"), 0, 2);
        grid.add(priorityBox, 1, 2);
        grid.add(new Label("Lead Investigator:"), 0, 3);
        grid.add(investigatorField, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descArea, 1, 4);

        pane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                CaseModel cm = new CaseModel();
                String num = caseNumField.getText();
                if (num == null || num.isBlank()) num = generatedCaseNum;
                String title = titleField.getText();
                if (title == null || title.isBlank()) title = "Investigation " + num.trim();

                cm.setCaseNumber(num.trim());
                cm.setTitle(title.trim());
                cm.setDescription(descArea.getText() != null ? descArea.getText().trim() : "");
                cm.setPriority(priorityBox.getValue() != null ? priorityBox.getValue() : CasePriority.HIGH);
                cm.setLeadInvestigator(investigatorField.getText() != null && !investigatorField.getText().isBlank()
                        ? investigatorField.getText().trim() : "Unassigned");
                cm.setStatus(CaseStatus.OPEN);
                return cm;
            }
            return null;
        });

        Optional<CaseModel> result = dialog.showAndWait();
        result.ifPresent(newCase -> {
            Task<CaseModel> task = new Task<>() {
                @Override
                protected CaseModel call() throws Exception {
                    return ServiceFactory.getInstance().getCaseService().createCase(newCase);
                }
            };
            task.setOnSucceeded(e -> {
                refreshData();
                UiUtils.showInfo("Case Created", "Case successfully registered: " + task.getValue().getCaseNumber());
            });
            task.setOnFailed(e -> UiUtils.showError("Failed to Create Case", task.getException().getMessage()));
            new Thread(task).start();
        });
    }

    @FXML
    private void onEditCase() {
        if (selectedCase == null) return;

        Dialog<CaseModel> dialog = new Dialog<>();
        dialog.setTitle("Edit Case Details");
        dialog.setHeaderText("Update Case Record: " + selectedCase.getCaseNumber());

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #CBD5E1;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(selectedCase.getTitle());
        TextArea descArea = new TextArea(selectedCase.getDescription() != null ? selectedCase.getDescription() : "");
        descArea.setPrefRowCount(3);

        ComboBox<CaseStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(CaseStatus.values()));
        statusBox.setValue(selectedCase.getStatus());

        ComboBox<CasePriority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(CasePriority.values()));
        priorityBox.setValue(selectedCase.getPriority());

        TextField investigatorField = new TextField(selectedCase.getLeadInvestigator() != null ? selectedCase.getLeadInvestigator() : "");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        grid.add(new Label("Priority:"), 0, 2);
        grid.add(priorityBox, 1, 2);
        grid.add(new Label("Investigator:"), 0, 3);
        grid.add(investigatorField, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descArea, 1, 4);

        pane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                selectedCase.setTitle(titleField.getText());
                selectedCase.setDescription(descArea.getText());
                selectedCase.setStatus(statusBox.getValue());
                selectedCase.setPriority(priorityBox.getValue());
                selectedCase.setLeadInvestigator(investigatorField.getText());
                return selectedCase;
            }
            return null;
        });

        Optional<CaseModel> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            Task<CaseModel> task = new Task<>() {
                @Override
                protected CaseModel call() throws Exception {
                    return ServiceFactory.getInstance().getCaseService().updateCase(updated);
                }
            };
            task.setOnSucceeded(e -> {
                refreshData();
                openCaseDetails(updated);
                UiUtils.showInfo("Case Updated", "Case " + updated.getCaseNumber() + " successfully updated.");
            });
            task.setOnFailed(e -> UiUtils.showError("Failed to Update Case", task.getException().getMessage()));
            new Thread(task).start();
        });
    }

    @FXML
    private void onDeleteCase() {
        if (selectedCase != null) {
            promptDeleteCase(selectedCase);
        }
    }

    private void promptDeleteCase(CaseModel targetCase) {
        if (targetCase == null) return;

        boolean confirm = UiUtils.showConfirmation(
                "Confirm Case Deletion",
                "Delete " + targetCase.getCaseNumber() + "?",
                "Are you sure you want to permanently delete case '" + targetCase.getTitle() + "'?\n" +
                        "This operation will permanently remove the case, its uploaded evidence, normalized events, and forensic reports from PostgreSQL."
        );

        if (!confirm) return;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return ServiceFactory.getInstance().getCaseService().deleteCase(targetCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                UiUtils.showInfo("Case Deleted", "Case " + targetCase.getCaseNumber() + " was deleted.");
                if (targetCase.equals(AppConfig.getInstance().getActiveCase())) {
                    AppConfig.getInstance().setActiveCase(null);
                }
                if (selectedCase != null && selectedCase.equals(targetCase)) {
                    onCloseDetails();
                }
                refreshData();
            } else {
                UiUtils.showError("Deletion Failed", "Backend rejected case deletion.");
            }
        });

        task.setOnFailed(e -> UiUtils.showError("Deletion Error", task.getException().getMessage()));
        new Thread(task).start();
    }
}
