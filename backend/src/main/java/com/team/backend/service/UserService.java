package com.team.backend.service;

import com.team.backend.entity.User;
import java.util.UUID;
import java.util.List;
import com.team.backend.dto.RegisterDto;
import com.team.backend.dto.LoginDto;
import com.team.backend.entity.User;
import com.team.backend.dto.UserDto;

public interface UserService {
    User createAdmin(String username, String rawPassword, boolean superAdmin);
    User findById(UUID id);
    User findByUsername(String username);
    List<User> findByRole(String role);
    UserDto register(RegisterDto dto);
    UserDto authenticate(LoginDto dto);
}
