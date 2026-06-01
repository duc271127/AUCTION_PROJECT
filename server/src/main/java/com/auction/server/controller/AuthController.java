package com.auction.server.controller;

import com.auction.server.dto.LoginByEmailDto;
import com.auction.server.dto.LoginDto;
import com.auction.server.dto.LoginResponse;
import com.auction.server.dto.RegisterByEmailDto;
import com.auction.server.dto.RegisterDto;
import com.auction.server.dto.UserDto;
import com.auction.server.service.JwtService;
import com.auction.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

