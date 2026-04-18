package com.team.backend.service.impl;

import com.team.backend.dto.LoginByEmailDto;
import com.team.backend.dto.RegisterByEmailDto;
import com.team.backend.dto.RegisterDto;
import com.team.backend.dto.LoginDto;
import com.team.backend.dto.UserDto;
import com.team.backend.entity.Admin;
import com.team.backend.entity.Bidder;
import com.team.backend.entity.Seller;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.UserRepository;
import com.team.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * UserServiceImpl with email-based register/login.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // existing methods (createAdmin, findById, findByUsername, findByRole)...
    @Override
    @Transactional
    public User createAdmin(String username, String rawPassword, boolean superAdmin) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessRuleException("Username is required");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessRuleException("Password must be at least 6 characters");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("Username already exists: " + username);
        }
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setSuperAdmin(superAdmin);
        admin.setRole("ADMIN");
        return userRepository.save(admin);
    }

    @Override
    public User findById(UUID id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User> findByRole(String role) {
        if (role == null) return List.of();
        return userRepository.findByRole(role);
    }

    // -------------------------
    // Email-based register / authenticate
    // -------------------------

    @Override
    @Transactional
    public UserDto registerByEmail(RegisterByEmailDto dto) {
        if (dto == null) throw new BusinessRuleException("Register payload is required");
        String email = dto.email == null ? null : dto.email.trim().toLowerCase();
        String password = dto.password;
        String role = dto.role == null ? null : dto.role.trim().toUpperCase();

        if (email == null || email.isEmpty()) {
            throw new BusinessRuleException("Email is required");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessRuleException("Password must be at least 6 characters");
        }
        if (!( "BIDDER".equals(role) || "SELLER".equals(role) || "ADMIN".equals(role) )) {
            throw new BusinessRuleException("Invalid role. Must be BIDDER, SELLER or ADMIN");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessRuleException("Email already registered: " + email);
        }

        User user;
        switch (role) {
            case "BIDDER":
                user = new Bidder();
                break;
            case "SELLER":
                user = new Seller();
                break;
            default:
                Admin admin = new Admin();
                admin.setSuperAdmin(false);
                user = admin;
                break;
        }

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Override
    public UserDto authenticateByEmail(LoginByEmailDto dto) {
        if (dto == null) throw new BusinessRuleException("Login payload is required");
        String email = dto.email == null ? null : dto.email.trim().toLowerCase();
        String password = dto.password;

        if (email == null || email.isEmpty() || password == null) {
            throw new BusinessRuleException("Invalid email or password");
        }

        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            throw new BusinessRuleException("Invalid email or password");
        }
        User user = opt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid email or password");
        }
        return toDto(user);
    }

    @Override
    public User findEntityByEmail(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional
    public UserDto register(RegisterDto dto) {
        if (dto == null) throw new BusinessRuleException("Register payload is required");
        String username = dto.username == null ? null : dto.username.trim();
        String password = dto.password;
        String role = dto.role == null ? null : dto.role.trim().toUpperCase();

        if (username == null || username.isEmpty()) {
            throw new BusinessRuleException("Username is required");
        }
        if (password == null || password.length() < 6) {
            throw new BusinessRuleException("Password must be at least 6 characters");
        }
        if (!( "BIDDER".equals(role) || "SELLER".equals(role) || "ADMIN".equals(role) )) {
            throw new BusinessRuleException("Invalid role. Must be BIDDER, SELLER or ADMIN");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessRuleException("Username already exists: " + username);
        }

        User user;
        switch (role) {
            case "BIDDER":
                user = new Bidder();
                break;
            case "SELLER":
                user = new Seller();
                break;
            default:
                Admin admin = new Admin();
                admin.setSuperAdmin(false);
                user = admin;
                break;
        }

        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Override
    public UserDto authenticate(LoginDto dto) {
        if (dto == null) throw new BusinessRuleException("Login payload is required");
        String username = dto.username == null ? null : dto.username.trim();
        String password = dto.password;

        if (username == null || username.isEmpty() || password == null) {
            throw new BusinessRuleException("Invalid username or password");
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            throw new BusinessRuleException("Invalid username or password");
        }
        User user = opt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("Invalid username or password");
        }
        return toDto(user);
    }

    // helper
    private UserDto toDto(User u) {
        if (u == null) return null;
        UserDto d = new UserDto();
        d.id = u.getId();
        d.username = u.getUsername();
        d.email = u.getEmail();
        d.role = u.getRole();
        return d;
    }
}
