package com.auction.server.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BIDDER")
public class Bidder extends User {
    public Bidder() { setRole("BIDDER"); }
}

