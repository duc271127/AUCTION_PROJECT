package com.auction.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base User entity (single table inheritance).
 * Extended with audit/security fields and helper methods.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username"}),
        @UniqueConstraint(columnNames = {"email"})
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {

    @Id
    private UUID id;

    @Column(nullable = true, unique = true, length = 100)
    @Size(max = 100)
    private String username;

    @Column(nullable = true, unique = true, length = 255)
    @Email
    @Size(max = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Role values: "BIDDER", "SELLER", "ADMIN"
     * Keep as String for compatibility; you can migrate to enum later.
     */
    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login")
    private Instant lastLogin;

    protected User() {
    }

    /**
     * Convenience constructor for creating a new user instance.
     * Note: passwordHash should already be encoded (BCrypt) before calling this constructor.
     */
    public User(String username,
                String email,
                String passwordHash,
                String role,
                String displayName) {

        this.id = UUID.randomUUID();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void prePersist() {

        if (this.id == null) {
            this.id = UUID.randomUUID();
        }

        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // -----------------------
    // Getters / Setters
    // -----------------------

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    // -----------------------
    // Helper methods
    // -----------------------

    /**
     * Convenience helper to set password hash from raw password using provided encoder.
     */
    public void setPasswordHashFromRaw(
            org.springframework.security.crypto.password.PasswordEncoder encoder,
            String rawPassword
    ) {

        if (encoder == null) {
            throw new IllegalArgumentException("PasswordEncoder is required");
        }

        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword is required");
        }

        this.passwordHash = encoder.encode(rawPassword);
    }

    /**
     * Check raw password against stored hash.
     */
    public boolean matchesPassword(
            org.springframework.security.crypto.password.PasswordEncoder encoder,
            String rawPassword
    ) {

        if (encoder == null) {
            throw new IllegalArgumentException("PasswordEncoder is required");
        }

        if (this.passwordHash == null) {
            return false;
        }

        return encoder.matches(rawPassword, this.passwordHash);
    }

    // -----------------------
    // equals / hashCode
    // -----------------------

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof User other)) {
            return false;
        }

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    // -----------------------
    // toString
    // -----------------------

    @Override
    public String toString() {

        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", displayName='" + displayName + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                '}';
    }
}

