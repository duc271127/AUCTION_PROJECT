package com.team.backend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

/**
 * Admin user. Can be extended with permissions later.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class  Admin extends User {
    @Column(name = "super_admin", nullable = true)
    private boolean superAdmin = false;

    public Admin() { setRole("ADMIN"); }

    public boolean isSuperAdmin() {
        return Boolean.TRUE.equals(superAdmin);
    }
    public void setSuperAdmin(boolean superAdmin) { this.superAdmin = superAdmin; }
}
