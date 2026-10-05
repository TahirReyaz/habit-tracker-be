package com.tally.domain;

import com.tally.crypto.Encrypted;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/** Per-day row data: rest day flag (休日) and a free journal line. */
@Entity
@Table(name = "day_meta")
public class DayMeta {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDate day;

    @Column(name = "rest_day", nullable = false)
    private boolean restDay;

    @Convert(converter = Encrypted.class)
    @Column(columnDefinition = "text")
    private String note;

    protected DayMeta() {}

    public DayMeta(UUID userId, LocalDate day) {
        this.userId = userId;
        this.day = day;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getDay() { return day; }
    public boolean isRestDay() { return restDay; }
    public void setRestDay(boolean restDay) { this.restDay = restDay; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
