package com.team.backend.service.impl;

import com.team.backend.entity.Admin;
import com.team.backend.entity.User;
import com.team.backend.exception.BusinessRuleException;
import com.team.backend.repository.UserRepository;
import com.team.backend.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User createAdmin(String username, String rawPassword, boolean superAdmin) {
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
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }
}
