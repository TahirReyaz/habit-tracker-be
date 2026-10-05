package com.tally.domain;

import com.tally.crypto.Encrypted;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One logged cell: habit x day. For BUILD habits it means "done", for AVOID habits it means "slipped". */
@Entity
@Table(name = "entries")
public class Entry {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(nullable = false)
    private LocalDate day;

    @Convert(converter = Encrypted.class)
    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Entry() {}

    public Entry(UUID userId, UUID habitId, LocalDate day) {
        this.userId = userId;
        this.habitId = habitId;
        this.day = day;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getHabitId() { return habitId; }
    public LocalDate getDay() { return day; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; this.updatedAt = Instant.now(); }
    public Instant getUpdatedAt() { return updatedAt; }
}
