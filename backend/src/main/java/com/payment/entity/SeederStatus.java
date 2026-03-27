package com.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Tracks whether the test data seeder has already run against this database.
 * The separate seeder container checks for a record with seederName="initial-seed"
 * and completed=true on startup. If found, it exits immediately to ensure
 * idempotent seeding — no duplicate Stripe entities are created across restarts.
 */
@Entity
@Table(name = "seeder_status")
public class SeederStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifies which seeder ran, e.g. "initial-seed" — allows multiple seeders in the future */
    private String seederName;

    /** Whether this seeder completed successfully */
    private boolean completed;

    /** When the seeder finished execution */
    private LocalDateTime executedAt;

    public SeederStatus() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSeederName() {
        return seederName;
    }

    public void setSeederName(String seederName) {
        this.seederName = seederName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
