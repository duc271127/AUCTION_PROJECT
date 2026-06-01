package com.auction.server.service;

import com.auction.server.dto.RegisterByEmailDto;
import com.auction.server.dto.LoginByEmailDto;
import com.auction.server.dto.LoginDto;
import com.auction.server.dto.RegisterDto;
import com.auction.server.entity.User;
import com.auction.server.dto.UserDto;
import java.util.List;
import java.util.UUID;

public interface UserService {
    User createAdmin(String username, String rawPassword, boolean superAdmin);
    User findById(UUID id);
    User findByUsername(String username);
    List<User> findByRole(String role);

    UserDto register(RegisterDto dto);
    UserDto authenticate(LoginDto dto);

    UserDto registerByEmail(RegisterByEmailDto dto);
    UserDto authenticateByEmail(LoginByEmailDto dto);
    User findEntityByEmail(String email);
}

