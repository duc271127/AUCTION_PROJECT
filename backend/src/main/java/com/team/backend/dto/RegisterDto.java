package com.team.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for user registration.
 */
public class RegisterDto {
    @NotBlank
    public String username;

    @NotBlank
    public String password;

    /**
     * Role string: "BIDDER", "SELLER", "ADMIN"
     */
    @NotBlank
    public String role;
}
