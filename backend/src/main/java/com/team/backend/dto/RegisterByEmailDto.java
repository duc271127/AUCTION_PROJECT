package com.team.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for register by email.
 */
public class RegisterByEmailDto {
    @Email
    @NotBlank
    public String email;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    public String password;

    /**
     * Role string: "BIDDER", "SELLER", "ADMIN"
     */
    @NotBlank
    public String role;
}
