package com.payment.seeder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks whether the seeder has already run to ensure idempotency.
 * The seeder checks for a completed record before doing any work.
 */
@Entity
@Table(name = "seeder_status")
public class SeederStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String seederName;
    private boolean completed;
    private LocalDateTime executedAt;

    public SeederStatus() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeederName() { return seederName; }
    public void setSeederName(String seederName) { this.seederName = seederName; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
