package com.investigation.controller;

import com.investigation.dto.CreateUserDto;
import com.investigation.dto.UserDto;
import com.investigation.service.ServiceFactory;
import com.investigation.service.SessionService;
import com.investigation.service.UserService;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Administration matching the reference design:
 * - 6 administration capability cards (Users, Data Management, System Settings,
 *   Case Templates, Audit Logs, Security)
 * - User provisioning and role-based access management
 */
public class AdminController implements MainLayoutController.Refreshable {

    @FXML private StackPane overlayPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterComboBox;

    @FXML private TableView<UserDto> usersTable;
    @FXML private TableColumn<UserDto, String> colUsername;
    @FXML private TableColumn<UserDto, String> colRole;
    @FXML private TableColumn<UserDto, String> colStatus;
    @FXML private TableColumn<UserDto, String> colCreatedAt;
    @FXML private TableColumn<UserDto, String> colId;

    @FXML private VBox detailPanel;
    @FXML private Label detailUsername;
    @FXML private Label detailRoleBadge;
    @FXML private Label detailUserId;
    @FXML private Label detailRole;
    @FXML private Label detailStatus;
    @FXML private Label detailCreatedAt;
    @FXML private Button btnDeleteUser;

    private final ObservableList<UserDto> masterUsers = FXCollections.observableArrayList();
    private FilteredList<UserDto> filteredUsers;

    @FXML
    public void initialize() {
        filteredUsers = new FilteredList<>(masterUsers, p -> true);

        roleFilterComboBox.getItems().setAll("ALL", "ADMIN", "INVESTIGATOR");
        roleFilterComboBox.setValue("ALL");

        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isEnabled() ? "ACTIVE" : "DISABLED"));
        colCreatedAt.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null ? DateTimeUtils.formatShortDate(data.getValue().getCreatedAt()) : "N/A"
        ));
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));

        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(role);
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        lbl.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-border-radius: 4px; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-border-radius: 4px; -fx-font-size: 11px;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(status);
                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        lbl.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-border-radius: 4px; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-size: 11px;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        usersTable.setItems(filteredUsers);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        roleFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showUserDetails(newVal);
        });

        refreshData();
    }

    private void updateFilter() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String roleFilter = roleFilterComboBox.getValue();

        filteredUsers.setPredicate(u -> {
            boolean matchesRole = roleFilter == null || "ALL".equalsIgnoreCase(roleFilter) ||
                    (u.getRole() != null && u.getRole().equalsIgnoreCase(roleFilter));

            if (!matchesRole) return false;
            if (query.isEmpty()) return true;

            return (u.getUsername() != null && u.getUsername().toLowerCase().contains(query)) ||
                    (u.getId() != null && u.getId().toLowerCase().contains(query)) ||
                    (u.getRole() != null && u.getRole().toLowerCase().contains(query));
        });
    }

    @Override
    public void refreshData() {
        Task<List<UserDto>> task = new Task<>() {
            @Override
            protected List<UserDto> call() throws Exception {
                UserService us = ServiceFactory.getInstance().getUserService();
                return us.getAllUsers();
            }
        };

        task.setOnSucceeded(e -> {
            List<UserDto> list = task.getValue();
            masterUsers.setAll(list);
            if (!list.isEmpty()) {
                usersTable.getSelectionModel().selectFirst();
            } else {
                showUserDetails(null);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            UiUtils.showError("Failed to Load Users", ex != null ? ex.getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void showUserDetails(UserDto user) {
        if (user == null) {
            detailUsername.setText("Select User");
            detailRoleBadge.setText("NONE");
            detailRoleBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;");
            detailUserId.setText("N/A");
            detailRole.setText("N/A");
            detailStatus.setText("N/A");
            detailCreatedAt.setText("N/A");
            btnDeleteUser.setDisable(true);
            return;
        }

        detailUsername.setText(user.getUsername());
        detailRoleBadge.setText(user.getRole() != null ? user.getRole() : "USER");
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            detailRoleBadge.setStyle("-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-border-color: #FECACA; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold;");
        } else {
            detailRoleBadge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-border-color: #BFDBFE; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold;");
        }

        detailUserId.setText(user.getId() != null ? user.getId() : "N/A");
        detailRole.setText(user.getRole() != null ? user.getRole() : "INVESTIGATOR");
        detailStatus.setText(user.isEnabled() ? "ACTIVE / ENABLED" : "DISABLED");
        detailStatus.setStyle(user.isEnabled() ? "-fx-text-fill: #059669; -fx-font-weight: bold;" : "-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        detailCreatedAt.setText(user.getCreatedAt() != null ? DateTimeUtils.formatUtc(user.getCreatedAt()) : "N/A");

        boolean isSelf = SessionService.getInstance().getCurrentSession() != null &&
                user.getUsername().equalsIgnoreCase(SessionService.getInstance().getCurrentSession().getUsername());
        btnDeleteUser.setDisable(isSelf);
    }

    // 6 Module Card Handlers
    @FXML
    private void onCardUsers() {
        usersTable.requestFocus();
    }

    @FXML
    private void onCardData() {
        UiUtils.showInfo("Data Management",
                "Automated database persistence and point-in-time recovery are managed through PostgreSQL 16 on port 5432.\n\nDatabase schema contains:\n• cases\n• evidence\n• normalized_events\n• analysis_reports\n• users");
    }

    @FXML
    private void onCardSettings() {
        UiUtils.showInfo("System Settings",
                "AI-Powered Digital Investigation Platform Services:\n• M1 Backend Service: Port 8080 (REST / JWT Auth)\n• M2 Evidence Processing Engine: Port 8081\n• M3 AI / Anomaly Detection Engine: Port 8082\n• Active Runtime: Java 21 + JavaFX 21");
    }

    @FXML
    private void onCardTemplates() {
        UiUtils.showInfo("Case Templates",
                "Standard forensic templates configured in system:\n1. Major Robbery & Theft\n2. Financial Fraud & Securities Scandal\n3. Industrial Disaster & Plant Telemetry\n4. Kidnapping & Physical Evidence Linkage\n5. Network Intrusion & Cloud Exfiltration");
    }

    @FXML
    private void onCardLogs() {
        UiUtils.showInfo("Audit Logs",
                "System Audit Logging Active:\nAll authentication logins, role evaluations, case creations, and evidence ingests are recorded with UTC timestamps, client IPs, and investigator IDs in PostgreSQL audit logs.");
    }

    @FXML
    private void onCardSecurity() {
        UiUtils.showInfo("Security Policy",
                "Platform Security Configuration:\n• Password Hashing: BCrypt (strength 10)\n• Authentication Tokens: Cryptographic bearer tokens\n• Role-Based Access Control: ADMIN and INVESTIGATOR enforcement\n• Local Network Boundary Isolation Active");
    }

    @FXML
    private void onNewUser() {
        Dialog<CreateUserDto> dialog = new Dialog<>();
        dialog.setTitle("Provision New User");
        dialog.setHeaderText("Create a new system account with designated security role.");

        ButtonType createBtnType = new ButtonType("Register Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #CBD5E1;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("e.g. examiner_jones");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Min 6 characters");

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("INVESTIGATOR", "ADMIN");
        roleCombo.setValue("INVESTIGATOR");

        CheckBox enabledCheck = new CheckBox("Account Enabled");
        enabledCheck.setSelected(true);

        grid.add(new Label("Username: *"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password: *"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Security Role:"), 0, 2);
        grid.add(roleCombo, 1, 2);
        grid.add(new Label("Status:"), 0, 3);
        grid.add(enabledCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(createBtnType);
        okBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6px 14px;");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createBtnType) {
                String u = usernameField.getText().trim();
                String p = passwordField.getText();
                String r = roleCombo.getValue();
                boolean en = enabledCheck.isSelected();
                if (u.length() < 3) {
                    UiUtils.showError("Validation Error", "Username must be at least 3 characters.");
                    return null;
                }
                if (p.length() < 6) {
                    UiUtils.showError("Validation Error", "Password must be at least 6 characters.");
                    return null;
                }
                return new CreateUserDto(u, p, r, en);
            }
            return null;
        });

        Optional<CreateUserDto> result = dialog.showAndWait();
        result.ifPresent(req -> {
            Task<UserDto> task = new Task<>() {
                @Override
                protected UserDto call() throws Exception {
                    UserService us = ServiceFactory.getInstance().getUserService();
                    return us.createUser(req);
                }
            };
            task.setOnSucceeded(e -> {
                UiUtils.showInfo("Success", "User '" + req.getUsername() + "' successfully registered.");
                refreshData();
            });
            task.setOnFailed(e -> {
                Throwable ex = task.getException();
                UiUtils.showError("Failed to Create User", ex != null ? ex.getMessage() : "Unknown error");
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        });
    }

    @FXML
    private void onDeleteUser() {
        UserDto selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean confirm = UiUtils.showConfirmation(
                "Confirm Deletion",
                "Delete user '" + selected.getUsername() + "'?",
                "This will revoke all system access for this account. This action cannot be undone."
        );

        if (!confirm) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                UserService us = ServiceFactory.getInstance().getUserService();
                us.deleteUser(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            UiUtils.showInfo("Account Deleted", "User '" + selected.getUsername() + "' was successfully removed.");
            refreshData();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            UiUtils.showError("Deletion Failed", ex != null ? ex.getMessage() : "Unknown error");
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
}
