package com.team.backend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

/**
 * Admin user. Can be extended with permissions later.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {
    @Column(nullable = false)
    private boolean superAdmin = false;

    public Admin() { setRole("ADMIN"); }

    public boolean isSuperAdmin() { return superAdmin; }
    public void setSuperAdmin(boolean superAdmin) { this.superAdmin = superAdmin; }
}
