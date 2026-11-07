package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.LoginRequest;
import com.cybertrace.m1.dto.LoginResponse;
import com.cybertrace.m1.entity.AuthTokenEntity;
import com.cybertrace.m1.entity.UserEntity;
import com.cybertrace.m1.exception.InvalidInputException;
import com.cybertrace.m1.repository.AuthTokenRepository;
import com.cybertrace.m1.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;

    @Value("${cybertrace.security.token-validity-seconds:86400}")
    private long tokenValiditySeconds;

    public AuthService(UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            PasswordEncoder passwordEncoder) {
        this(userRepository, authTokenRepository, passwordEncoder, new SecurityAuditService());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            PasswordEncoder passwordEncoder,
            SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditService = securityAuditService;
    }

    public LoginResponse login(LoginRequest request) {
        return login(request, "unknown");
    }

    public LoginResponse login(LoginRequest request, String clientIp) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            securityAuditService.logLoginFailure("<blank>", clientIp, "Blank username");
            throw new InvalidInputException("Username cannot be blank");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            securityAuditService.logLoginFailure(request.getUsername(), clientIp, "Blank password");
            throw new InvalidInputException("Password cannot be blank");
        }

        String username = request.getUsername().trim();

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: user '{}' not found", username);
                    securityAuditService.logLoginFailure(username, clientIp, "User not found");
                    return new BadCredentialsException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Authentication failed: invalid password for user '{}'", username);
            securityAuditService.logLoginFailure(username, clientIp, "Invalid password credentials");
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            log.warn("Authentication rejected: account '{}' is disabled", username);
            securityAuditService.logLoginFailure(username, clientIp, "Account disabled");
            throw new DisabledException("User account is disabled. Contact system administrator.");
        }

        AuthTokenEntity tokenEntity = new AuthTokenEntity(user, tokenValiditySeconds);
        AuthTokenEntity saved = authTokenRepository.save(tokenEntity);

        log.info("User '{}' successfully authenticated with role {}", user.getUsername(), user.getRole());
        securityAuditService.logLoginSuccess(user.getUsername(), clientIp);

        return new LoginResponse(
                saved.getToken(),
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                saved.getExpiresAt());
    }

    public void logout(String token) {
        logout(token, "unknown");
    }

    public void logout(String token, String clientIp) {
        if (token == null || token.isBlank())
            return;
        authTokenRepository.findById(token).ifPresent(t -> {
            t.setRevoked(true);
            authTokenRepository.save(t);
            log.info("Token revoked for user '{}'", t.getUser().getUsername());
            securityAuditService.logLogout(t.getUser().getUsername(), clientIp);
        });
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return authTokenRepository.findByTokenAndRevokedFalse(token)
                .filter(AuthTokenEntity::isValid)
                .map(AuthTokenEntity::getUser);
    }
}
