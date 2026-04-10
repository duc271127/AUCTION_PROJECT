package com.team.backend.service;

import com.team.backend.entity.User;
import java.util.UUID;
import java.util.List;

public interface UserService {
    User createAdmin(String username, String rawPassword, boolean superAdmin);
    User findById(UUID id);
    User findByUsername(String username);
    List<User> findByRole(String role);
}
