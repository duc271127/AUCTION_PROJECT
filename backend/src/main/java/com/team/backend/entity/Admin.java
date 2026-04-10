package com.team.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;

@Entity
public class Admin extends User {

    // ví dụ: một flag để bật/tắt siêu quyền (super admin)
    @Column(nullable = false)
    private boolean superAdmin = false;

    public Admin() {
        super();
        this.setRole("ADMIN");
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }
}
