package com.tally.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/** A habit's target for one specific week, overriding its usual weekly target. */
@Entity
@Table(name = "week_targets")
public class WeekTarget {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private short target;

    protected WeekTarget() {}

    public WeekTarget(UUID userId, UUID habitId, LocalDate weekStart) {
        this.userId = userId;
        this.habitId = habitId;
        this.weekStart = weekStart;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getHabitId() { return habitId; }
    public LocalDate getWeekStart() { return weekStart; }
    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = (short) target; }
}
