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

import java.util.List;
import java.util.Optional;

public class CasesController implements MainLayoutController.Refreshable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TableView<CaseModel> casesTable;
    @FXML private TableColumn<CaseModel, String> colCaseNum;
    @FXML private TableColumn<CaseModel, String> colTitle;
    @FXML private TableColumn<CaseModel, String> colStatus;
    @FXML private TableColumn<CaseModel, String> colPriority;
    @FXML private TableColumn<CaseModel, String> colInvestigator;
    @FXML private TableColumn<CaseModel, String> colEvidence;
    @FXML private TableColumn<CaseModel, String> colEvents;
    @FXML private TableColumn<CaseModel, String> colCreated;

    // Side detail panel
    @FXML private VBox detailPanel;
    @FXML private Label detailCaseNumber;
    @FXML private Label detailTitle;
    @FXML private Label detailDescription;
    @FXML private Label detailInvestigator;
    @FXML private Label detailCreated;
    @FXML private Label detailUpdated;
    @FXML private HBox detailBadges;
    @FXML private Button setActiveButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

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
        colCaseNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCaseNumber()));
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));

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

        colInvestigator.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLeadInvestigator() != null ? d.getValue().getLeadInvestigator() : "Unassigned"));
        colEvidence.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEvidenceCount())));
        colEvents.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEventCount())));
        colCreated.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.formatShortDate(d.getValue().getCreatedAt())));

        casesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showCaseDetails(newVal);
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
                } else {
                    casesTable.getSelectionModel().selectFirst();
                }
            } else {
                showCaseDetails(null);
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

    private void showCaseDetails(CaseModel cm) {
        this.selectedCase = cm;
        if (cm == null) {
            detailCaseNumber.setText("No Case Selected");
            detailTitle.setText("Select a case from the table to view details");
            detailDescription.setText("");
            detailInvestigator.setText("N/A");
            detailCreated.setText("N/A");
            detailUpdated.setText("N/A");
            detailBadges.getChildren().clear();
            setActiveButton.setDisable(true);
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        detailCaseNumber.setText(cm.getCaseNumber());
        detailTitle.setText(cm.getTitle());
        detailDescription.setText(cm.getDescription() != null ? cm.getDescription() : "No case description provided.");
        detailInvestigator.setText(cm.getLeadInvestigator() != null ? cm.getLeadInvestigator() : "Unassigned");
        detailCreated.setText(DateTimeUtils.formatUtc(cm.getCreatedAt()));
        detailUpdated.setText(DateTimeUtils.formatUtc(cm.getUpdatedAt()));

        detailBadges.getChildren().setAll(
                UiUtils.createStatusBadge(cm.getStatus()),
                UiUtils.createPriorityBadge(cm.getPriority())
        );

        setActiveButton.setDisable(false);
        editButton.setDisable(false);
        deleteButton.setDisable(false);
    }

    @FXML
    private void onSetActiveCase() {
        if (selectedCase != null) {
            AppConfig.getInstance().setActiveCase(selectedCase);
            UiUtils.showInfo("Active Case Updated", "Case " + selectedCase.getCaseNumber() + " is now the active workstation focus.");
        }
    }

    @FXML
    private void onNewCase() {
        Dialog<CaseModel> dialog = new Dialog<>();
        dialog.setTitle("Create New Investigation Case");
        dialog.setHeaderText("Create Case Record (POST /api/cases)");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        pane.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #F8FAFC;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Unauthorized Cloud Egress");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Case scope and initial incident description...");
        descArea.setPrefRowCount(3);

        ComboBox<CasePriority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(CasePriority.values()));
        priorityBox.setValue(CasePriority.HIGH);

        TextField investigatorField = new TextField();
        investigatorField.setPromptText("Special Agent / Analyst name");

        grid.add(new Label("Case Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(priorityBox, 1, 1);
        grid.add(new Label("Investigator:"), 0, 2);
        grid.add(investigatorField, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descArea, 1, 3);

        pane.setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                CaseModel cm = new CaseModel();
                cm.setTitle(titleField.getText());
                cm.setDescription(descArea.getText());
                cm.setPriority(priorityBox.getValue());
                cm.setLeadInvestigator(investigatorField.getText());
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
        dialog.setHeaderText("Update Case " + selectedCase.getCaseNumber() + " (PUT /api/cases/{id})");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        pane.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #F8FAFC;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
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
                UiUtils.showInfo("Case Updated", "Case " + updated.getCaseNumber() + " successfully updated.");
            });
            task.setOnFailed(e -> UiUtils.showError("Failed to Update Case", task.getException().getMessage()));
            new Thread(task).start();
        });
    }

    @FXML
    private void onDeleteCase() {
        if (selectedCase == null) return;

        boolean confirm = UiUtils.showConfirmation(
                "Confirm Case Deletion",
                "Delete " + selectedCase.getCaseNumber() + "?",
                "Are you sure you want to permanently delete case '" + selectedCase.getTitle() + "'?\n" +
                        "This will call DELETE /api/cases/" + selectedCase.getId() + " on the backend."
        );

        if (!confirm) return;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return ServiceFactory.getInstance().getCaseService().deleteCase(selectedCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                UiUtils.showInfo("Case Deleted", "Case " + selectedCase.getCaseNumber() + " was deleted.");
                if (selectedCase.equals(AppConfig.getInstance().getActiveCase())) {
                    AppConfig.getInstance().setActiveCase(null);
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
