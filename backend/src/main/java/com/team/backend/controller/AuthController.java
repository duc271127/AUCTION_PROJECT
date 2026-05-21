package com.team.backend.controller;

import com.team.backend.dto.LoginByEmailDto;
import com.team.backend.dto.LoginDto;
import com.team.backend.dto.LoginResponse;
import com.team.backend.dto.RegisterByEmailDto;
import com.team.backend.dto.RegisterDto;
import com.team.backend.dto.UserDto;
import com.team.backend.service.JwtService;
import com.team.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterDto dto) {
        UserDto created = userService.register(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/register-email")
    public ResponseEntity<UserDto> registerByEmail(@Valid @RequestBody RegisterByEmailDto dto) {
        UserDto created = userService.registerByEmail(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginDto dto) {
        UserDto user = userService.authenticate(dto);
        return ResponseEntity.ok(toLoginResponse(user));
    }

    @PostMapping("/login-email")
    public ResponseEntity<LoginResponse> loginByEmail(@Valid @RequestBody LoginByEmailDto dto) {
        UserDto user = userService.authenticateByEmail(dto);
        return ResponseEntity.ok(toLoginResponse(user));
    }

    private LoginResponse toLoginResponse(UserDto user) {
        return new LoginResponse(
                user.id,
                user.username,
                user.email,
                user.role,
                jwtService.generateToken(user)
        );
    }
}