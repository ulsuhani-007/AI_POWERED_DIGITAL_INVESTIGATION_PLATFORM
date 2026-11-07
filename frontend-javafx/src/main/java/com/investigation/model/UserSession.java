package com.investigation.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Client-side authenticated user session.
 * Stores only token and user profile metadata required by the client.
 * Passwords are never stored or exposed.
 */
public class UserSession {

    private final String token;
    private final String userId;
    private final String username;
    private final String role;
    private final Instant expiresAt;

    public UserSession(String token, String userId, String username, String role, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role != null ? role.toUpperCase() : "INVESTIGATOR";
        this.expiresAt = expiresAt != null ? expiresAt : Instant.now().plusSeconds(86400);
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSession that = (UserSession) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public String toString() {
        return "UserSession{username='" + username + "', role='" + role + "'}";
    }
}

