package com.cybertrace.m1.dto;

import com.cybertrace.m1.entity.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    @JsonProperty("username")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    @JsonProperty("password")
    private String password;

    @NotNull(message = "Role is required")
    @JsonProperty("role")
    private UserRole role = UserRole.INVESTIGATOR;

    @JsonProperty("enabled")
    private boolean enabled = true;

    public CreateUserRequest() {}

    public CreateUserRequest(String username, String password, UserRole role, boolean enabled) {
        this.username = username;
        this.password = password;
        this.role = role != null ? role : UserRole.INVESTIGATOR;
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "CreateUserRequest{username='" + username + "', role=" + role + ", enabled=" + enabled + ", password=[PROTECTED]}";
    }
}

