package com.team.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for login.
 */
public class LoginDto {
    @NotBlank
    public String username;

    @NotBlank
    public String password;
}
