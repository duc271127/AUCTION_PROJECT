package com.team.backend.config;

import com.team.backend.entity.Admin;
import com.team.backend.entity.User;
import com.team.backend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Configuration
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfNotExists("admin1@example.com", "Admin@123", "Admin One");
        createAdminIfNotExists("admin2@example.com", "Admin@456", "Admin Two");
    }

    private void createAdminIfNotExists(String email, String rawPassword, String displayName) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) return;

        Admin u = new Admin();
        u.setEmail(email);
        // dùng setter đúng tên (passwordHash) hoặc helper
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole("ADMIN"); // hoặc "ROLE_ADMIN" tùy convention project
        u.setDisplayName(displayName);
        u.setCreatedAt(Instant.now());

        userRepository.save(u);
        System.out.println("Created admin account: " + email);
    }
}
