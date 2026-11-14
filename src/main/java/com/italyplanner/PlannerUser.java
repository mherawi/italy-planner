package com.italyplanner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "planner_users")
public class PlannerUser {
    @Id
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    protected PlannerUser() {
    }

    PlannerUser(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    String passwordHash() {
        return passwordHash;
    }
}
