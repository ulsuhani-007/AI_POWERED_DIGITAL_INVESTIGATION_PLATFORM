package com.investigation.service;

import com.investigation.api.ApiClient;
import com.investigation.dto.LoginRequest;
import com.investigation.dto.LoginResponse;
import com.investigation.model.UserSession;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Centralized Session and Authentication Service for the JavaFX Workstation.
 * Manages active user sessions, token injection, and session expiration events.
 */
public class SessionService {

    private static final SessionService INSTANCE = new SessionService();

    private final ObjectProperty<UserSession> currentSession = new SimpleObjectProperty<>(null);
    private final List<Consumer<UserSession>> sessionListeners = new ArrayList<>();
    private Runnable unauthorizedHandler = null;

    private SessionService() {}

    public static SessionService getInstance() {
        return INSTANCE;
    }

    public boolean isLoggedIn() {
        UserSession session = currentSession.get();
        return session != null && !session.isExpired();
    }

    public UserSession getCurrentSession() {
        return currentSession.get();
    }

    public ObjectProperty<UserSession> currentSessionProperty() {
        return currentSession;
    }

    public String getToken() {
        UserSession session = currentSession.get();
        return session != null ? session.getToken() : null;
    }

    public String getAuthHeader() {
        String token = getToken();
        return (token != null && !token.isBlank()) ? "Bearer " + token : null;
    }

    public UserSession login(String username, String password, ApiClient apiClient) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        LoginResponse resp = apiClient.post("/api/auth/login", req, LoginResponse.class);

        if (resp == null || resp.getToken() == null || resp.getToken().isBlank()) {
            throw new IllegalStateException("Authentication failed: server returned empty token");
        }

        UserSession session = new UserSession(
                resp.getToken(),
                resp.getUserId(),
                resp.getUsername(),
                resp.getRole(),
                resp.getExpiresAt()
        );

        this.currentSession.set(session);
        notifyListeners(session);
        return session;
    }

    public void logout(ApiClient apiClient) {
        try {
            if (apiClient != null && isLoggedIn()) {
                apiClient.post("/api/auth/logout", null, Void.class);
            }
        } catch (Exception ignored) {
            // Best effort logout on backend
        } finally {
            clearSession();
            if (unauthorizedHandler != null) {
                Platform.runLater(unauthorizedHandler);
            }
        }
    }

    public void clearSession() {
        this.currentSession.set(null);
        notifyListeners(null);
    }

    public void handleUnauthorized() {
        clearSession();
        Platform.runLater(() -> {
            if (unauthorizedHandler != null) {
                unauthorizedHandler.run();
            }
            UiUtils.showWarning("Session Expired", "Your authentication session has expired or is invalid. Please sign in again.");
        });
    }

    public void handleForbidden() {
        Platform.runLater(() -> {
            UiUtils.showError("Access Denied", "You do not have permission to perform this action.");
        });
    }

    public void setUnauthorizedHandler(Runnable handler) {
        this.unauthorizedHandler = handler;
    }

    public void addSessionListener(Consumer<UserSession> listener) {
        this.sessionListeners.add(listener);
    }

    private void notifyListeners(UserSession session) {
        for (Consumer<UserSession> l : sessionListeners) {
            try {
                l.accept(session);
            } catch (Exception ignored) {}
        }
    }
}
