package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.CaseModel;
import com.investigation.model.EventModel;
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimelineController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventTypeComboBox;
    @FXML private ToggleButton sortOrderToggle; // Newest / Oldest
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
    @FXML private Label detailEventId;
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
                sortOrderToggle.setText(newVal ? "⏱️ Newest First" : "⏱️ Oldest First");
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
                    (evt.getDevice() != null && evt.getDevice().toLowerCase().contains(query)) ||
                    (evt.getId() != null && evt.getId().toLowerCase().contains(query));
        });

        eventCountLabel.setText("Showing " + filteredEvents.size() + " of " + masterEvents.size() + " events");
    }

    private void setupListView() {
        eventsListView.setItems(sortedEvents);
        eventsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EventModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(createEventCard(item));
                    setText(null);
                }
            }
        });

        eventsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showEventDetails(newVal);
        });
    }

    private VBox createEventCard(EventModel evt) {
        VBox card = new VBox(4);
        card.getStyleClass().add("timeline-card");

        // Top line: Time + Event Type Badge + ID
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(DateTimeUtils.formatUtc(evt.getTimestamp()));
        timeLabel.getStyleClass().add("timeline-time");

        Label typeBadge = new Label(evt.getEventType() != null ? evt.getEventType() : "EVENT");
        typeBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38BDF8; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label idLabel = new Label("ID: " + evt.getId());
        idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569; -fx-font-family: monospace;");

        top.getChildren().addAll(typeBadge, timeLabel, spacer, idLabel);

        // Description
        Label descLabel = new Label(evt.getDescription() != null ? evt.getDescription() : "No event description.");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #F8FAFC; -fx-font-weight: 500;");
        descLabel.setWrapText(true);

        // Metadata Pills (User, Device, IP, Bytes)
        HBox pills = new HBox(12);
        pills.setAlignment(Pos.CENTER_LEFT);

        if (evt.getUser() != null && !evt.getUser().isBlank()) {
            Label u = new Label("👤 " + evt.getUser());
            u.setStyle("-fx-font-size: 10px; -fx-text-fill: #00F0FF;");
            pills.getChildren().add(u);
        }
        if (evt.getDevice() != null && !evt.getDevice().isBlank()) {
            Label d = new Label("💻 " + evt.getDevice());
            d.setStyle("-fx-font-size: 10px; -fx-text-fill: #A855F7;");
            pills.getChildren().add(d);
        }
        if (evt.getIpAddress() != null && !evt.getIpAddress().isBlank()) {
            Label ip = new Label("🌐 " + evt.getIpAddress());
            ip.setStyle("-fx-font-size: 10px; -fx-text-fill: #F59E0B;");
            pills.getChildren().add(ip);
        }
        if (evt.getBytesTransferred() != null && evt.getBytesTransferred() > 0) {
            String bytesText = evt.getBytesTransferred() > 1024 * 1024
                    ? String.format("%.2f MB", evt.getBytesTransferred() / (1024.0 * 1024.0))
                    : evt.getBytesTransferred() + " B";
            Label b = new Label("📊 " + bytesText);
            b.setStyle("-fx-font-size: 10px; -fx-text-fill: #10B981;");
            pills.getChildren().add(b);
        }

        card.getChildren().addAll(top, descLabel, pills);
        return card;
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
            activeCaseHeaderLabel.setText("NO ACTIVE CASE SELECTED");
            masterEvents.clear();
            showEventDetails(null);
            return;
        }

        final CaseModel currentCase = activeCase;
        activeCaseHeaderLabel.setText("CHRONOLOGICAL TIMELINE FOR " + currentCase.getCaseNumber() + " : " + currentCase.getTitle());

        showLoading("Extracting normalized event stream (GET /api/cases/" + currentCase.getId() + "/events)...");

        Task<List<EventModel>> task = new Task<>() {
            @Override
            protected List<EventModel> call() throws Exception {
                EventService service = ServiceFactory.getInstance().getEventService();
                return service.getEventsByCase(currentCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            List<EventModel> list = task.getValue();
            masterEvents.setAll(list);

            // Populate event type filter combo
            Set<String> types = new HashSet<>();
            types.add("ALL");
            for (EventModel ev : list) {
                if (ev.getEventType() != null && !ev.getEventType().isBlank()) {
                    types.add(ev.getEventType());
                }
            }
            eventTypeComboBox.getItems().setAll(types);
            eventTypeComboBox.setValue("ALL");

            eventCountLabel.setText("Showing " + list.size() + " of " + list.size() + " events");

            if (!list.isEmpty()) {
                eventsListView.getSelectionModel().selectFirst();
            } else {
                showEventDetails(null);
            }
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch Events", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void showEventDetails(EventModel evt) {
        if (evt == null) {
            detailEventType.setText("No Event Selected");
            detailTimestamp.setText("N/A");
            detailUser.setText("N/A");
            detailDevice.setText("N/A");
            detailIp.setText("N/A");
            detailSuccess.setText("N/A");
            detailBytes.setText("N/A");
            detailEvidenceId.setText("N/A");
            detailEventId.setText("N/A");
            detailDescriptionArea.setText("Select an event from the timeline to view normalized parameters.");
            return;
        }

        detailEventType.setText(evt.getEventType() != null ? evt.getEventType() : "EVENT");
        detailTimestamp.setText(DateTimeUtils.formatUtc(evt.getTimestamp()));
        detailUser.setText(evt.getUser() != null ? evt.getUser() : "None / Anonymous");
        detailDevice.setText(evt.getDevice() != null ? evt.getDevice() : "Unspecified");
        detailIp.setText(evt.getIpAddress() != null ? evt.getIpAddress() : "Local Loopback / N/A");
        detailSuccess.setText(evt.getSuccess() != null ? (evt.getSuccess() ? "SUCCESS" : "FAILURE") : "N/A");
        detailBytes.setText(evt.getBytesTransferred() != null ? evt.getBytesTransferred() + " bytes" : "0 bytes");
        detailEvidenceId.setText(evt.getEvidenceId() != null ? evt.getEvidenceId() : "N/A");
        detailEventId.setText(evt.getId() != null ? evt.getId() : "N/A");
        detailDescriptionArea.setText(evt.getDescription() != null ? evt.getDescription() : "");
    }
}
