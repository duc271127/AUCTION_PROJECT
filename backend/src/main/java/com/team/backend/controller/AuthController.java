package com.team.backend.controller;

import com.team.backend.dto.RegisterDto;
import com.team.backend.dto.LoginDto;
import com.team.backend.dto.RegisterByEmailDto;
import com.team.backend.dto.LoginByEmailDto;
import com.team.backend.dto.UserDto;
import com.team.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Authentication endpoints: register and login.
 * For Phase 2 we return simple UserDto on success.
 * Later you can replace login to return JWT token.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) { this.userService = userService; }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterDto dto) {
        UserDto created = userService.register(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginDto dto) {
        UserDto user = userService.authenticate(dto);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register-email")
    public ResponseEntity<UserDto> registerByEmail(@Valid @RequestBody RegisterByEmailDto dto) {
        UserDto created = userService.registerByEmail(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login-email")
    public ResponseEntity<UserDto> loginByEmail(@Valid @RequestBody LoginByEmailDto dto) {
        UserDto user = userService.authenticateByEmail(dto);
        return ResponseEntity.ok(user);
    }
}
