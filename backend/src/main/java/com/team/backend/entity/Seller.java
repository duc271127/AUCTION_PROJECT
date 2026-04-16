package com.team.backend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SELLER")
public class Seller extends User {
    public Seller() { setRole("SELLER"); }
}
