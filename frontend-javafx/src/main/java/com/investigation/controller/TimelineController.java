package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.EventModel;
import com.investigation.model.enums.Severity;
import com.investigation.service.EventService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for Chronological Forensic Timeline matching the reference design:
 * - Clean vertical timeline with timestamps, event types, IP addresses, and severity badges
 * - Instant filtering by search query and category
 * - Detail panel for selected event
 */
public class TimelineController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventTypeComboBox;
    @FXML private ToggleButton sortOrderToggle;
    @FXML private Label eventCountLabel;

    @FXML private ListView<EventModel> eventsListView;

    // Detail side panel
    @FXML private VBox detailPanel;
    @FXML private Label detailEventType;
    @FXML private Label detailTimestamp;
    @FXML private Label detailUser;
    @FXML private Label detailDevice;
    @FXML private Label detailIp;
    @FXML private Label detailSuccess;
    @FXML private Label detailBytes;
    @FXML private Label detailEvidenceId;
    @FXML private TextArea detailDescriptionArea;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;

    private final ObservableList<EventModel> masterEvents = FXCollections.observableArrayList();
    private FilteredList<EventModel> filteredEvents;
    private SortedList<EventModel> sortedEvents;

    @FXML
    public void initialize() {
        setupFilteringAndSorting();
        setupListView();
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

    private void setupFilteringAndSorting() {
        filteredEvents = new FilteredList<>(masterEvents, e -> true);

        sortedEvents = new SortedList<>(filteredEvents, (e1, e2) -> {
            boolean newestFirst = sortOrderToggle == null || sortOrderToggle.isSelected();
            int cmp = e1.getTimestamp().compareTo(e2.getTimestamp());
            return newestFirst ? -cmp : cmp;
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        eventTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());

        if (sortOrderToggle != null) {
            sortOrderToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                sortOrderToggle.setText(newVal ? "⏱ Newest First" : "⏱ Oldest First");
                sortedEvents.setComparator((e1, e2) -> {
                    int cmp = e1.getTimestamp().compareTo(e2.getTimestamp());
                    return newVal ? -cmp : cmp;
                });
            });
        }
    }

    private void updateFilter() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String typeFilter = eventTypeComboBox.getValue();

        filteredEvents.setPredicate(evt -> {
            boolean matchesType = typeFilter == null || "ALL".equalsIgnoreCase(typeFilter) ||
                    (evt.getEventType() != null && evt.getEventType().equalsIgnoreCase(typeFilter));

            if (!matchesType) return false;
            if (query.isEmpty()) return true;

            return (evt.getDescription() != null && evt.getDescription().toLowerCase().contains(query)) ||
                    (evt.getEventType() != null && evt.getEventType().toLowerCase().contains(query)) ||
                    (evt.getUser() != null && evt.getUser().toLowerCase().contains(query)) ||
                    (evt.getIpAddress() != null && evt.getIpAddress().toLowerCase().contains(query)) ||
                    (evt.getDevice() != null && evt.getDevice().toLowerCase().contains(query));
        });

        eventCountLabel.setText("Showing " + filteredEvents.size() + " of " + masterEvents.size() + " events");
    }

    private void setupListView() {
        Label placeholder = new Label("No timeline events available for this case.\nUpload supported evidence to extract chronological telemetry.");
        placeholder.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-alignment: center; -fx-text-alignment: center;");
        eventsListView.setPlaceholder(placeholder);

        eventsListView.setItems(sortedEvents);
        eventsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EventModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createEventCard(item));
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 3px 0;");
                }
            }
        });

        eventsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showEventDetails(newVal);
        });
    }

    private HBox createEventCard(EventModel evt) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10px 14px; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-cursor: hand;");

        // Hover effect
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 10px 14px; -fx-background-radius: 8px; -fx-border-color: #CBD5E1; -fx-border-radius: 8px; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10px 14px; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-cursor: hand;"));

        // Left circular timeline node dot
        StackPane nodeDot = new StackPane();
        nodeDot.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 50%; -fx-min-width: 10px; -fx-min-height: 10px; -fx-max-width: 10px; -fx-max-height: 10px;");

        // Time label (e.g. 12:46:32)
        String timeStr = DateTimeUtils.formatTimeOnly(evt.getTimestamp());
        Label timeLabel = new Label(timeStr != null ? timeStr : "12:00:00");
        timeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0F172A; -fx-min-width: 75px;");

        // Separator vertical bar
        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sep.setPrefHeight(20);

        // Action title: e.g. "Suspicious Login | 192.168.1.10" or "File Access | flight.png"
        String actionType = evt.getEventType() != null ? evt.getEventType().replace("_", " ") : "Event";
        String target = evt.getIpAddress() != null ? evt.getIpAddress() :
                (evt.getFileName() != null ? evt.getFileName() : (evt.getUser() != null ? evt.getUser() : "System"));
        Label actionLabel = new Label(actionType + "  |  " + target);
        actionLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Severity badge
        Severity severity = determineSeverity(evt);
        Label sevBadge = UiUtils.createSeverityBadge(severity);

        row.getChildren().addAll(nodeDot, timeLabel, sep, actionLabel, spacer, sevBadge);
        return row;
    }

    private Severity determineSeverity(EventModel evt) {
        String desc = evt.getDescription() != null ? evt.getDescription().toLowerCase() : "";
        String type = evt.getEventType() != null ? evt.getEventType().toUpperCase() : "";

        if (desc.contains("breach") || desc.contains("ransom") || desc.contains("unauthorized") || desc.contains("pressure spike") || desc.contains("abduction")) {
            return Severity.HIGH;
        } else if (desc.contains("suspicious") || desc.contains("clearing") || desc.contains("off-hours") || type.contains("LOGIN")) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }

    private void showEventDetails(EventModel evt) {
        if (evt == null) {
            detailEventType.setText("EVENT");
            detailTimestamp.setText("--");
            detailUser.setText("--");
            detailDevice.setText("--");
            detailIp.setText("--");
            detailSuccess.setText("--");
            detailBytes.setText("--");
            detailEvidenceId.setText("--");
            detailDescriptionArea.setText("Select an event from the timeline feed to inspect normalized telemetry.");
            return;
        }

        detailEventType.setText(evt.getEventType() != null ? evt.getEventType() : "EVENT");
        detailTimestamp.setText(DateTimeUtils.formatUtc(evt.getTimestamp()));
        detailUser.setText(evt.getUser() != null ? evt.getUser() : "System / Automated");
        detailDevice.setText(evt.getDevice() != null ? evt.getDevice() : "Local Device");
        detailIp.setText(evt.getIpAddress() != null ? evt.getIpAddress() : "127.0.0.1");
        detailSuccess.setText(evt.getSuccess() != null && evt.getSuccess() ? "SUCCESS (200 OK)" : "FLAGGED / WARN");
        detailBytes.setText(evt.getBytesTransferred() != null ? evt.getBytesTransferred() + " bytes transferred" : "N/A");
        detailEvidenceId.setText(evt.getEvidenceId() != null ? "Ref: " + evt.getEvidenceId() : "Manual Ingest");
        detailDescriptionArea.setText(evt.getDescription() != null ? evt.getDescription() : "No detailed description.");
    }

    @FXML
    @Override
    public void refreshData() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            activeCaseHeaderLabel.setText("[No Case Selected]");
            masterEvents.clear();
            showEventDetails(null);
            return;
        }

        activeCaseHeaderLabel.setText("[" + activeCase.getCaseNumber() + " - " + activeCase.getTitle() + "]");
        showLoading("Loading chronological events for " + activeCase.getCaseNumber() + "...");

        Task<List<EventModel>> task = new Task<>() {
            @Override
            protected List<EventModel> call() throws Exception {
                EventService service = ServiceFactory.getInstance().getEventService();
                return service.getEventsByCase(activeCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            List<EventModel> list = task.getValue();
            masterEvents.setAll(list);

            // Populate event types in combo box
            Set<String> types = new HashSet<>();
            types.add("ALL");
            for (EventModel evt : list) {
                if (evt.getEventType() != null) types.add(evt.getEventType());
            }
            eventTypeComboBox.getItems().setAll(types);
            eventTypeComboBox.setValue("ALL");

            if (!masterEvents.isEmpty()) {
                eventsListView.getSelectionModel().selectFirst();
            } else {
                showEventDetails(null);
            }
            eventCountLabel.setText("Showing " + masterEvents.size() + " events");
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch Timeline", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onJumpToGraph() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().navigateToScreen("RelationshipGraph");
        }
    }
}
