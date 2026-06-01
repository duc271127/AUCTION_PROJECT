package com.auction.server.config;

import com.auction.server.entity.Admin;
import com.auction.server.entity.User;
import com.auction.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Configuration
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        createAdminIfNotExists(
                "admin1",
                "admin1@example.com",
                "Admin@123",
                "Admin One"
        );

        createAdminIfNotExists(
                "admin2",
                "admin2@example.com",
                "Admin@456",
                "Admin Two"
        );
    }

    private void createAdminIfNotExists(
            String username,
            String email,
            String rawPassword,
            String displayName
    ) {

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() || userRepository.findByUsername(username).isPresent()) {
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setRole("ADMIN");
        admin.setDisplayName(displayName);
        admin.setCreatedAt(Instant.now());

        userRepository.save(admin);
        LOGGER.info("Created admin account {}", email);
    }
}

