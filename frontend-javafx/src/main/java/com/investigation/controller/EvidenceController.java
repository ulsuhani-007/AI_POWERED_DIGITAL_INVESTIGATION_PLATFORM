package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.EvidenceModel;
import com.investigation.model.enums.EvidenceStatus;
import com.investigation.service.EvidenceService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Evidence management matching the reference design:
 * - Table view with file type icons, Evidence ID, File Name, Collector, Date, and Status badges
 * - Evidence detail & inspection card with SHA-256 hash and metadata
 * - Operable upload modal dialog with FileChooser and progress feedback
 */
public class EvidenceController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private MenuButton filterMenuButton;
    @FXML private Button tableViewBtn;
    @FXML private Button galleryViewBtn;

    @FXML private TableView<EvidenceModel> evidenceTable;
    @FXML private TableColumn<EvidenceModel, Void> colIcon;
    @FXML private TableColumn<EvidenceModel, String> colEvidenceId;
    @FXML private TableColumn<EvidenceModel, String> colFileName;
    @FXML private TableColumn<EvidenceModel, String> colType;
    @FXML private TableColumn<EvidenceModel, String> colCase;
    @FXML private TableColumn<EvidenceModel, String> colCollector;
    @FXML private TableColumn<EvidenceModel, String> colDate;
    @FXML private TableColumn<EvidenceModel, String> colStatus;

    // Details panel
    @FXML private VBox detailsBox;
    @FXML private Label detailFileName;
    @FXML private Label detailStatus;
    @FXML private Label detailType;
    @FXML private Label detailSize;
    @FXML private Label detailDate;
    @FXML private Label detailEvents;
    @FXML private Label detailHash;
    @FXML private Label detailDescription;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;
    private final ObservableList<EvidenceModel> masterEvidence = FXCollections.observableArrayList();
    private FilteredList<EvidenceModel> filteredEvidence;
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
        // Icon column
        colIcon.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    EvidenceModel em = getTableRow().getItem();
                    String fn = em.getFileName().toLowerCase();
                    String icon = fn.endsWith(".png") || fn.endsWith(".jpg") ? "🖼️" :
                            fn.endsWith(".pdf") ? "📕" :
                            fn.endsWith(".html") ? "🌐" :
                            fn.endsWith(".txt") || fn.endsWith(".log") ? "📄" : "📊";
                    Label l = new Label(icon);
                    l.setStyle("-fx-font-size: 16px; -fx-alignment: center;");
                    setGraphic(l);
                }
            }
        });

        // Evidence ID (e.g. EVD-001 ->)
        colEvidenceId.setCellValueFactory(d -> {
            String id = d.getValue().getId();
            String display = id != null && id.length() > 8 ? "EVD-" + id.substring(0, 4).toUpperCase() + " →" : "EVD-001 →";
            return new SimpleStringProperty(display);
        });

        colFileName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileName()));
        colType.setCellValueFactory(d -> {
            String t = d.getValue().getFileType();
            return new SimpleStringProperty(t != null ? t.toUpperCase() : "CSV");
        });

        colCase.setCellValueFactory(d -> {
            CaseModel active = AppConfig.getInstance().getActiveCase();
            return new SimpleStringProperty(active != null ? active.getCaseNumber() : "C-001");
        });

        colCollector.setCellValueFactory(d -> {
            CaseModel active = AppConfig.getInstance().getActiveCase();
            String inv = active != null && active.getLeadInvestigator() != null ? active.getLeadInvestigator() : "Unassigned";
            return new SimpleStringProperty(inv);
        });

        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUploadedAt() != null ? DateTimeUtils.formatShortDate(d.getValue().getUploadedAt()) : "N/A"
        ));

        // Status badge: Verified/Processed in green, Analyzed in blue, Failed in red
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    EvidenceModel em = getTableRow().getItem();
                    Label badge;
                    if (em.getStatus() == EvidenceStatus.FAILED) {
                        badge = UiUtils.createBadge("Processing Failed", "#991B1B", "#FEE2E2", "#FECACA");
                    } else if (em.getStatus() == EvidenceStatus.ANALYZED) {
                        badge = UiUtils.createBadge("Analyzed", "#1E40AF", "#DBEAFE", "#BFDBFE");
                    } else if (em.getEventCount() > 0) {
                        badge = UiUtils.createBadge("Processed", "#065F46", "#D1FAE5", "#A7F3D0");
                    } else {
                        badge = UiUtils.createBadge("Uploaded", "#92400E", "#FEF3C7", "#FDE68A");
                    }
                    setGraphic(badge);
                }
            }
        });

        evidenceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showEvidenceDetails(newVal);
        });

        filteredEvidence = new FilteredList<>(masterEvidence, p -> true);
        evidenceTable.setItems(filteredEvidence);
    }

    private void showEvidenceDetails(EvidenceModel em) {
        this.selectedEvidence = em;
        if (em == null) {
            detailFileName.setText("Evidence Details");
            detailStatus.setText("Ready");
            detailStatus.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-border-color: #CBD5E1; -fx-border-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
            detailType.setText("--");
            detailSize.setText("--");
            detailDate.setText("--");
            detailEvents.setText("--");
            detailHash.setText("--");
            detailDescription.setText("Select an evidence artifact to inspect chain of custody metadata.");
            return;
        }

        detailFileName.setText(em.getFileName());
        if (em.getStatus() == EvidenceStatus.FAILED) {
            detailStatus.setText("Processing Failed");
            detailStatus.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-border-color: #FECACA; -fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else if (em.getStatus() == EvidenceStatus.ANALYZED) {
            detailStatus.setText("Analyzed");
            detailStatus.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-border-color: #BFDBFE; -fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else if (em.getEventCount() > 0) {
            detailStatus.setText("Processed");
            detailStatus.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-border-color: #A7F3D0; -fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            detailStatus.setText("Uploaded");
            detailStatus.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-border-color: #FDE68A; -fx-padding: 3px 10px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        detailType.setText(em.getFileType() != null ? em.getFileType().toUpperCase() + " Artifact" : "Forensic Manifest");
        detailSize.setText(em.getFormattedSize());
        detailDate.setText(em.getUploadedAt() != null ? DateTimeUtils.formatUtc(em.getUploadedAt()) : "N/A");
        detailEvents.setText(em.getEventCount() + " normalized events");
        detailHash.setText(em.getHashSha256() != null ? em.getHashSha256() : "SHA-256 pending verification");
        detailDescription.setText(em.getDescription() != null && !em.getDescription().isBlank()
                ? em.getDescription() : "Forensic evidence artifact uploaded to investigation repository.");
    }

    @FXML
    @Override
    public void refreshData() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            activeCaseHeaderLabel.setText("[No Case Selected]");
            masterEvidence.clear();
            showEvidenceDetails(null);
            return;
        }

        activeCaseHeaderLabel.setText("[" + activeCase.getCaseNumber() + " - " + activeCase.getTitle() + "]");
        showLoading("Loading evidence artifacts for " + activeCase.getCaseNumber() + "...");

        Task<List<EvidenceModel>> task = new Task<>() {
            @Override
            protected List<EvidenceModel> call() throws Exception {
                EvidenceService service = ServiceFactory.getInstance().getEvidenceService();
                return service.getEvidenceByCase(activeCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            masterEvidence.setAll(task.getValue());
            if (!masterEvidence.isEmpty()) {
                evidenceTable.getSelectionModel().selectFirst();
            } else {
                showEvidenceDetails(null);
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

    // Filter handlers
    @FXML private void onFilterAll() { filteredEvidence.setPredicate(p -> true); }
    @FXML private void onFilterVerified() { filteredEvidence.setPredicate(e -> e.getStatus() == EvidenceStatus.ANALYZED || e.getEventCount() > 0); }
    @FXML private void onFilterProcessing() { filteredEvidence.setPredicate(e -> e.getStatus() != EvidenceStatus.ANALYZED && e.getEventCount() == 0); }

    @FXML private void onShowTableView() {
        tableViewBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5px 12px; -fx-background-radius: 4px;");
        galleryViewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: 600; -fx-font-size: 11px; -fx-padding: 5px 12px;");
    }

    @FXML private void onShowGalleryView() {
        galleryViewBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5px 12px; -fx-background-radius: 4px;");
        tableViewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-weight: 600; -fx-font-size: 11px; -fx-padding: 5px 12px;");
    }

    @FXML
    private void onJumpToTimelineForEvidence() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("Timeline");
        }
    }

    @FXML
    private void onOpenUploadDialog() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            UiUtils.showWarning("No Case Selected", "Please select an active investigation case before uploading evidence.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Upload Evidence Artifact");
        dialog.setHeaderText("Ingest New Forensic Evidence into " + activeCase.getCaseNumber());

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #CBD5E1;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField filePathField = new TextField();
        filePathField.setPromptText("No file selected...");
        filePathField.setEditable(false);
        filePathField.setPrefWidth(260);

        Button browseBtn = new Button("Browse File...");
        browseBtn.getStyleClass().add("btn-secondary");

        final File[] selectedFile = new File[1];
        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Evidence File (CSV, LOG, PCAP)");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Evidence Files (*.csv, *.log, *.txt)", "*.csv", "*.log", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File f = chooser.showOpenDialog(dialog.getOwner());
            if (f != null) {
                selectedFile[0] = f;
                filePathField.setText(f.getName() + " (" + (f.length() / 1024) + " KB)");
            }
        });

        TextField noteField = new TextField();
        noteField.setPromptText("Collector name / chain of custody notes...");

        grid.add(new Label("Select File: *"), 0, 0);
        grid.add(new HBox(8, filePathField, browseBtn), 1, 0);
        grid.add(new Label("Description / Note:"), 0, 1);
        grid.add(noteField, 1, 1);

        pane.setContent(grid);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Upload & Ingest");
        okBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6px 14px;");

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && selectedFile[0] != null) {
            File f = selectedFile[0];
            String note = noteField.getText();

            showLoading("Uploading & normalizing " + f.getName() + "...");
            Task<EvidenceModel> uploadTask = new Task<>() {
                @Override
                protected EvidenceModel call() throws Exception {
                    EvidenceService es = ServiceFactory.getInstance().getEvidenceService();
                    return es.uploadEvidence(activeCase.getId(), f, note, progress -> {});
                }
            };

            uploadTask.setOnSucceeded(ev -> {
                hideLoading();
                refreshData();
                EvidenceModel em = uploadTask.getValue();
                if (em != null && em.getStatus() == EvidenceStatus.FAILED) {
                    UiUtils.showWarning("Evidence Processing Notice",
                            "Evidence '" + em.getFileName() + "' was securely stored with SHA-256 integrity hash (" +
                            (em.getHashSha256() != null ? em.getHashSha256().substring(0, Math.min(16, em.getHashSha256().length())) + "..." : "recorded") +
                            "), but automated event normalization could not identify a supported forensic schema (Login, Network, File Access, Browser Activity).\n\n" +
                            "Chain of custody record has been safely preserved.");
                } else if (em != null) {
                    UiUtils.showInfo("Evidence Ingested",
                            "Evidence '" + em.getFileName() + "' successfully ingested and normalized into " +
                            em.getEventCount() + " chronological events with verified SHA-256 integrity hash.");
                }
            });

            uploadTask.setOnFailed(ev -> {
                hideLoading();
                UiUtils.showError("Upload Failed", uploadTask.getException() != null ? uploadTask.getException().getMessage() : "Unknown error");
            });

            new Thread(uploadTask).start();
        }
    }
}
