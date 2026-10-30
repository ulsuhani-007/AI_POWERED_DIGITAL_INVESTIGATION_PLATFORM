package com.investigation.config;

import com.investigation.model.CaseModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Global configuration and state management for the JavaFX Workstation.
 */
public class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final StringProperty backendBaseUrl = new SimpleStringProperty("http://localhost:8080");
    private final BooleanProperty mockModeEnabled = new SimpleBooleanProperty(true); // Default to mock mode for reliable demo
    private final ObjectProperty<CaseModel> activeCase = new SimpleObjectProperty<>(null);
    private final BooleanProperty backendConnected = new SimpleBooleanProperty(false);
    private final StringProperty systemStatusMessage = new SimpleStringProperty("SYSTEM INITIALIZED");

    private AppConfig() {}

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getBackendBaseUrl() {
        return backendBaseUrl.get();
    }

    public void setBackendBaseUrl(String url) {
        this.backendBaseUrl.set(url);
    }

    public StringProperty backendBaseUrlProperty() {
        return backendBaseUrl;
    }

    public boolean isMockModeEnabled() {
        return mockModeEnabled.get();
    }

    public void setMockModeEnabled(boolean enabled) {
        this.mockModeEnabled.set(enabled);
    }

    public BooleanProperty mockModeEnabledProperty() {
        return mockModeEnabled;
    }

    public CaseModel getActiveCase() {
        return activeCase.get();
    }

    public void setActiveCase(CaseModel caseModel) {
        this.activeCase.set(caseModel);
    }

    public ObjectProperty<CaseModel> activeCaseProperty() {
        return activeCase;
    }

    public boolean isBackendConnected() {
        return backendConnected.get();
    }

    public void setBackendConnected(boolean connected) {
        this.backendConnected.set(connected);
    }

    public BooleanProperty backendConnectedProperty() {
        return backendConnected;
    }

    public String getSystemStatusMessage() {
        return systemStatusMessage.get();
    }

    public void setSystemStatusMessage(String message) {
        this.systemStatusMessage.set(message);
    }

    public StringProperty systemStatusMessageProperty() {
        return systemStatusMessage;
    }
}
