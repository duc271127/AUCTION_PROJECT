package com.team.backend.config;

import com.team.backend.entity.Admin;
import com.team.backend.entity.Bidder;
import com.team.backend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Configuration
public class DemoAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfMissing("admin", "admin@example.com", "Admin@123", "Admin");
        createBidderIfMissing("bidder", "bidder@example.com", "123456", "Bidder");
        createBidderIfMissing("bidder1", "bidder1@example.com", "123456", "Bidder One");
    }

    private void createAdminIfMissing(String username, String email, String rawPassword, String displayName) {
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
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
    }

    private void createBidderIfMissing(String username, String email, String rawPassword, String displayName) {
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
            return;
        }

        Bidder bidder = new Bidder();
        bidder.setUsername(username);
        bidder.setEmail(email);
        bidder.setPasswordHash(passwordEncoder.encode(rawPassword));
        bidder.setRole("BIDDER");
        bidder.setDisplayName(displayName);
        bidder.setCreatedAt(Instant.now());
        userRepository.save(bidder);
    }
}
