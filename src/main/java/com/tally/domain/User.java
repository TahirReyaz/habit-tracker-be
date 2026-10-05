package com.tally.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "week_start", nullable = false)
    private short weekStart = 1;

    @Column(name = "lock_pin_hash")
    private String lockPinHash;

    @Column(name = "lock_minutes", nullable = false)
    private short lockMinutes = 0;

    @Column(name = "discreet_default", nullable = false)
    private boolean discreetDefault = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public int getWeekStart() { return weekStart; }
    public void setWeekStart(int weekStart) { this.weekStart = (short) weekStart; }
    public String getLockPinHash() { return lockPinHash; }
    public void setLockPinHash(String lockPinHash) { this.lockPinHash = lockPinHash; }
    public int getLockMinutes() { return lockMinutes; }
    public void setLockMinutes(int lockMinutes) { this.lockMinutes = (short) lockMinutes; }
    public boolean isDiscreetDefault() { return discreetDefault; }
    public void setDiscreetDefault(boolean discreetDefault) { this.discreetDefault = discreetDefault; }
    public Instant getCreatedAt() { return createdAt; }
}
