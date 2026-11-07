package com.cybertrace.m1.controller;

import com.cybertrace.m1.dto.LoginRequest;
import com.cybertrace.m1.dto.LoginResponse;
import com.cybertrace.m1.dto.UserResponseDTO;
import com.cybertrace.m1.entity.UserEntity;
import com.cybertrace.m1.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Secure authentication, session token issuance, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Validates username and password against BCrypt hash and returns a cryptographic session token.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = resolveClientIp(servletRequest);
        LoginResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate active session", description = "Revokes the active bearer session token immediately.")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        String clientIp = resolveClientIp(request);
        if (token != null) {
            authService.logout(token, clientIp);
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "Session invalidated successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated investigator", description = "Returns active user profile for the validated session token.")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt()
        ));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7).trim();
        }
        String xAuth = request.getHeader("X-Auth-Token");
        if (xAuth != null && !xAuth.isBlank()) {
            return xAuth.trim();
        }
        return null;
    }
}

