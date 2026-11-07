package com.cybertrace.m1.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an active or revoked authentication session token.
 */
@Entity
@Table(name = "auth_tokens", indexes = {
        @Index(name = "idx_token_expires", columnList = "expires_at"),
        @Index(name = "idx_token_user_id", columnList = "user_id")
})
public class AuthTokenEntity {

    @Id
    @Column(name = "token", length = 64, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    public AuthTokenEntity() {
        this.token = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plusSeconds(86400); // 24 hours validity
        this.revoked = false;
    }

    public AuthTokenEntity(UserEntity user, long validitySeconds) {
        this();
        this.user = user;
        this.expiresAt = this.createdAt.plusSeconds(validitySeconds);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(Instant.now()) && user != null && user.isEnabled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthTokenEntity that = (AuthTokenEntity) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}

