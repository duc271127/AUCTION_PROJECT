package com.team.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for login by email.
 */
public class LoginByEmailDto {
    @Email
    @NotBlank
    public String email;

    @NotBlank
    public String password;
}
