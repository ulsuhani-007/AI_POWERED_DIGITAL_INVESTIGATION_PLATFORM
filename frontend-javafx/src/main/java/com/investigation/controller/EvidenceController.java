package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.EvidenceModel;
import com.investigation.service.EvidenceService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class EvidenceController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private TableView<EvidenceModel> evidenceTable;
    @FXML private TableColumn<EvidenceModel, String> colFileName;
    @FXML private TableColumn<EvidenceModel, String> colType;
    @FXML private TableColumn<EvidenceModel, String> colSize;
    @FXML private TableColumn<EvidenceModel, String> colDate;
    @FXML private TableColumn<EvidenceModel, String> colStatus;
    @FXML private TableColumn<EvidenceModel, String> colEvents;
    @FXML private TableColumn<EvidenceModel, String> colHash;

    // Details panel
    @FXML private VBox detailsBox;
    @FXML private Label detailFileName;
    @FXML private Label detailStatus;
    @FXML private Label detailSize;
    @FXML private Label detailDate;
    @FXML private Label detailSource;
    @FXML private Label detailHash;
    @FXML private Label detailDescription;

    // Upload section
    @FXML private TextField selectedFilePathField;
    @FXML private TextField descriptionField;
    @FXML private Button selectFileButton;
    @FXML private Button uploadButton;
    @FXML private ProgressBar uploadProgressBar;
    @FXML private Label uploadStatusLabel;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;
    private File chosenFile = null;
    private EvidenceModel selectedEvidence = null;

    @FXML
    public void initialize() {
        setupTable();
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
        colFileName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileName()));
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileType()));
        colSize.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedSize()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.formatShortDate(d.getValue().getUploadedAt())));

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    EvidenceModel em = getTableRow().getItem();
                    setGraphic(UiUtils.createBadge(em.getStatus().getDisplayName(), em.getStatus().getColorHex(), "#0F172A"));
                }
            }
        });

        colEvents.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEventCount())));
        colHash.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getHashSha256() != null && d.getValue().getHashSha256().length() > 16
                        ? d.getValue().getHashSha256().substring(0, 16) + "..."
                        : (d.getValue().getHashSha256() != null ? d.getValue().getHashSha256() : "N/A")
        ));

        evidenceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showDetails(newVal);
        });
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
            activeCaseHeaderLabel.setText("NO ACTIVE CASE SELECTED — SELECT A CASE TO VIEW EVIDENCE");
            evidenceTable.getItems().clear();
            showDetails(null);
            uploadButton.setDisable(true);
            selectFileButton.setDisable(true);
            return;
        }

        final CaseModel currentCase = activeCase;
        activeCaseHeaderLabel.setText("EVIDENCE ARTIFACTS FOR " + currentCase.getCaseNumber() + " : " + currentCase.getTitle());
        uploadButton.setDisable(chosenFile == null);
        selectFileButton.setDisable(false);

        showLoading("Querying case evidence (GET /api/cases/" + currentCase.getId() + "/evidence)...");

        Task<List<EvidenceModel>> task = new Task<>() {
            @Override
            protected List<EvidenceModel> call() throws Exception {
                EvidenceService service = ServiceFactory.getInstance().getEvidenceService();
                return service.getEvidenceByCase(currentCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            List<EvidenceModel> list = task.getValue();
            evidenceTable.getItems().setAll(list);
            if (!list.isEmpty()) {
                evidenceTable.getSelectionModel().selectFirst();
            } else {
                showDetails(null);
            }
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch Evidence", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void showDetails(EvidenceModel em) {
        this.selectedEvidence = em;
        if (em == null) {
            detailFileName.setText("No Evidence Selected");
            detailStatus.setText("N/A");
            detailSize.setText("N/A");
            detailDate.setText("N/A");
            detailSource.setText("N/A");
            detailHash.setText("N/A");
            detailDescription.setText("Select an evidence item from the table to inspect metadata.");
            return;
        }

        detailFileName.setText(em.getFileName());
        detailStatus.setText(em.getStatus().getDisplayName());
        detailSize.setText(em.getFormattedSize());
        detailDate.setText(DateTimeUtils.formatUtc(em.getUploadedAt()));
        detailSource.setText(em.getSource() != null ? em.getSource() : "Unknown");
        detailHash.setText(em.getHashSha256() != null ? em.getHashSha256() : "N/A");
        detailDescription.setText(em.getDescription() != null ? em.getDescription() : "No evidence notes.");
    }

    @FXML
    private void onSelectFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CSV Evidence File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Evidence Files (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Log & PCAP Files (*.log, *.pcap, *.txt)", "*.log", "*.pcap", "*.txt"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        File file = chooser.showOpenDialog(evidenceTable.getScene().getWindow());
        if (file != null) {
            this.chosenFile = file;
            selectedFilePathField.setText(file.getAbsolutePath() + " (" + (file.length() / 1024) + " KB)");
            uploadButton.setDisable(false);
            uploadStatusLabel.setText("Ready to upload: " + file.getName());
            uploadStatusLabel.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 11px;");
        }
    }

    @FXML
    private void onUploadEvidence() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            UiUtils.showError("Upload Error", "No active case is selected. Please select a case first.");
            return;
        }

        if (chosenFile == null || !chosenFile.exists()) {
            UiUtils.showError("Upload Error", "Please select a valid CSV evidence file.");
            return;
        }

        boolean isMock = AppConfig.getInstance().isMockModeEnabled();
        boolean isConnected = AppConfig.getInstance().isBackendConnected();

        if (!isMock && !isConnected) {
            UiUtils.showError("Backend Unavailable",
                    "Cannot upload evidence: No backend server is connected at " +
                            AppConfig.getInstance().getBackendBaseUrl() + ".\n\n" +
                            "Please start the Spring Boot backend or switch to Simulated Mock Mode in the header.");
            uploadStatusLabel.setText("Upload aborted: Backend disconnected.");
            uploadStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
            return;
        }

        uploadButton.setDisable(true);
        selectFileButton.setDisable(true);
        uploadProgressBar.setVisible(true);
        uploadProgressBar.setProgress(0.0);
        uploadStatusLabel.setText("Uploading " + chosenFile.getName() + " to POST /api/cases/" + activeCase.getId() + "/evidence...");
        uploadStatusLabel.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 11px;");

        String description = descriptionField.getText();

        Task<EvidenceModel> uploadTask = new Task<>() {
            @Override
            protected EvidenceModel call() throws Exception {
                EvidenceService service = ServiceFactory.getInstance().getEvidenceService();
                return service.uploadEvidence(activeCase.getId(), chosenFile, description, progress -> {
                    Platform.runLater(() -> uploadProgressBar.setProgress(progress));
                });
            }
        };

        uploadTask.setOnSucceeded(e -> {
            uploadProgressBar.setProgress(1.0);
            uploadStatusLabel.setText("Upload & Ingestion Complete: " + chosenFile.getName());
            uploadStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px;");
            uploadButton.setDisable(false);
            selectFileButton.setDisable(false);
            chosenFile = null;
            selectedFilePathField.clear();
            descriptionField.clear();
            refreshData();
        });

        uploadTask.setOnFailed(e -> {
            uploadProgressBar.setVisible(false);
            uploadStatusLabel.setText("Upload failed: " + uploadTask.getException().getMessage());
            uploadStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px;");
            uploadButton.setDisable(false);
            selectFileButton.setDisable(false);
            UiUtils.showError("Upload Failed", uploadTask.getException().getMessage());
        });

        Thread th = new Thread(uploadTask);
        th.setDaemon(true);
        th.start();
    }
}
