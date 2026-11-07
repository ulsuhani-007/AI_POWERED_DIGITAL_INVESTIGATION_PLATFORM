package com.cybertrace.m1.security;

import com.cybertrace.m1.entity.UserEntity;
import com.cybertrace.m1.entity.enums.UserRole;
import com.cybertrace.m1.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Initializes default administrative and forensic investigator accounts
 * with secure BCrypt password hashes if no users exist in the database.
 */
@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${cybertrace.security.default-admin-password:AdminPass123!}")
    private String defaultAdminPassword;

    @Value("${cybertrace.security.default-investigator-password:InvestigatorPass123!}")
    private String defaultInvestigatorPassword;

    public SecurityDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("No user accounts found in database. Seeding initial platform users...");

            // Seed default administrator
            UserEntity admin = new UserEntity(
                    "admin",
                    passwordEncoder.encode(defaultAdminPassword),
                    UserRole.ADMIN,
                    true
            );
            admin.setCreatedAt(Instant.now());
            userRepository.save(admin);

            // Seed default investigator
            UserEntity investigator = new UserEntity(
                    "investigator",
                    passwordEncoder.encode(defaultInvestigatorPassword),
                    UserRole.INVESTIGATOR,
                    true
            );
            investigator.setCreatedAt(Instant.now());
            userRepository.save(investigator);

            log.info("Default accounts created with BCrypt password hashes: 'admin' (ROLE_ADMIN) and 'investigator' (ROLE_INVESTIGATOR).");
        }
    }
}

