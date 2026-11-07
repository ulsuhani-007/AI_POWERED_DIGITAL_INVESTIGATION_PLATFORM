package com.investigation.controller;

import com.investigation.api.ApiClient;
import com.investigation.model.UserSession;
import com.investigation.service.ServiceFactory;
import com.investigation.service.SessionService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.function.Consumer;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusMessageLabel;
    @FXML private Button signInButton;
    @FXML private ProgressBar loginProgressBar;

    private Consumer<UserSession> onLoginSuccessCallback;

    public void setOnLoginSuccess(Consumer<UserSession> callback) {
        this.onLoginSuccessCallback = callback;
    }

    @FXML
    public void initialize() {
        // Clear status when typing
        usernameField.textProperty().addListener((obs, o, n) -> clearStatus());
        passwordField.textProperty().addListener((obs, o, n) -> clearStatus());
    }

    @FXML
    private void onSignIn() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isBlank()) {
            showError("Please enter your analyst username.");
            usernameField.requestFocus();
            return;
        }

        if (password == null || password.isBlank()) {
            showError("Please enter your security credential password.");
            passwordField.requestFocus();
            return;
        }

        setLoading(true);

        Task<UserSession> loginTask = new Task<>() {
            @Override
            protected UserSession call() throws Exception {
                ApiClient apiClient = ServiceFactory.getInstance().getApiClient();
                return SessionService.getInstance().login(username.trim(), password, apiClient);
            }
        };

        loginTask.setOnSucceeded(e -> {
            setLoading(false);
            UserSession session = loginTask.getValue();
            passwordField.clear();

            if (onLoginSuccessCallback != null) {
                onLoginSuccessCallback.accept(session);
            }
        });

        loginTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = loginTask.getException();
            String msg = ex != null ? ex.getMessage() : "Authentication failed";
            if (msg.contains("Invalid username or password") || msg.contains("401")) {
                showError("Invalid analyst credentials. Check username and password.");
            } else if (msg.contains("disabled") || msg.contains("403")) {
                showError("Access Denied: This investigator account is currently disabled.");
            } else if (msg.contains("Unable to connect") || msg.contains("Connection refused")) {
                showError("Backend Offline: Cannot connect to http://localhost:8080. Verify cybertrace-m1 is running.");
            } else {
                showError("Authentication Error: " + msg);
            }
        });

        Thread th = new Thread(loginTask);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onSelectInvestigatorPreset() {
        usernameField.setText("investigator");
        passwordField.setText("InvestigatorPass123!");
        clearStatus();
    }

    @FXML
    private void onSelectAdminPreset() {
        usernameField.setText("admin");
        passwordField.setText("AdminPass123!");
        clearStatus();
    }

    private void setLoading(boolean loading) {
        signInButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loginProgressBar.setVisible(loading);
        loginProgressBar.setManaged(loading);
    }

    private void showError(String msg) {
        statusMessageLabel.setText("⚠️ " + msg);
        statusMessageLabel.setStyle("-fx-text-fill: #EF4444; -fx-background-color: rgba(239, 68, 68, 0.12); -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 12px; -fx-background-radius: 6px;");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void clearStatus() {
        statusMessageLabel.setText("");
        statusMessageLabel.setVisible(false);
        statusMessageLabel.setManaged(false);
    }
}

