package com.tally.domain;

import com.tally.crypto.Encrypted;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One logged cell: habit x day. For BUILD habits it means "done", for AVOID habits it means "slipped" — whatever the input type. */
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

    /** SELECT: the chosen option(s), newline separated. TEXT: the text itself. CHECK: null. Encrypted. */
    @Convert(converter = Encrypted.class)
    @Column(columnDefinition = "text")
    private String value;

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
    public String getValue() { return value; }
    /** The picked options of a SELECT entry (one, or several for multi-select habits). */
    public java.util.List<String> getValues() {
        return value == null || value.isEmpty() ? java.util.List.of() : java.util.Arrays.asList(value.split("\n"));
    }
    public void setValue(String value) { this.value = value; this.updatedAt = Instant.now(); }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; this.updatedAt = Instant.now(); }
    public Instant getUpdatedAt() { return updatedAt; }
}
