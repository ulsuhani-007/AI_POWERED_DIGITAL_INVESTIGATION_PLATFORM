package com.cybertrace.m1.service;

import com.cybertrace.m1.dto.CreateUserRequest;
import com.cybertrace.m1.dto.UserResponseDTO;
import com.cybertrace.m1.entity.UserEntity;
import com.cybertrace.m1.exception.DuplicateResourceException;
import com.cybertrace.m1.exception.ResourceNotFoundException;
import com.cybertrace.m1.repository.AuthTokenRepository;
import com.cybertrace.m1.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService securityAuditService;

    public UserService(UserRepository userRepository,
                       AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this(userRepository, authTokenRepository, passwordEncoder, new SecurityAuditService());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserService(UserRepository userRepository,
                       AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder,
                       SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditService = securityAuditService;
    }

    public UserResponseDTO createUser(CreateUserRequest request) {
        String cleanUsername = request.getUsername().trim();
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new DuplicateResourceException("Username '" + cleanUsername + "' is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        UserEntity user = new UserEntity(
                cleanUsername,
                passwordHash,
                request.getRole(),
                request.isEnabled()
        );
        user.setCreatedAt(Instant.now());

        UserEntity saved = userRepository.save(user);
        log.info("Created new user account '{}' with role {}", saved.getUsername(), saved.getRole());

        String adminUsername = resolveCurrentAdmin();
        securityAuditService.logUserCreated(adminUsername, saved.getUsername(), saved.getRole().name());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(String id) {
        return userRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public void deleteUser(String id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        authTokenRepository.revokeAllForUser(user.getId());
        userRepository.delete(user);
        log.info("Deleted user account '{}' (id: {})", user.getUsername(), id);

        String adminUsername = resolveCurrentAdmin();
        securityAuditService.logUserDeleted(adminUsername, user.getUsername());
    }

    private String resolveCurrentAdmin() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserEntity user) {
                return user.getUsername();
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                return ud.getUsername();
            } else if (principal instanceof String s && !s.isBlank()) {
                return s;
            }
            return auth.getName();
        }
        return "admin";
    }

    private UserResponseDTO mapToDTO(UserEntity entity) {
        return new UserResponseDTO(
                entity.getId(),
                entity.getUsername(),
                entity.getRole().name(),
                entity.isEnabled(),
                entity.getCreatedAt()
        );
    }
}

