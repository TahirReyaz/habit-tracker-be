package com.tally.domain;

import com.tally.crypto.Encrypted;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "habits")
public class Habit {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = Encrypted.class)
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, length = 40)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HabitKind kind = HabitKind.BUILD;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private InputType inputType = InputType.CHECK;

    /** Option labels for SELECT habits, newline separated, encrypted at rest. */
    @Convert(converter = Encrypted.class)
    @Column(columnDefinition = "text")
    private String options;

    /** SELECT only: allow several options on the same day. */
    @Column(name = "multi_select", nullable = false)
    private boolean multiSelect;

    @Column(name = "weekly_target", nullable = false)
    private short weeklyTarget;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    @Column(nullable = false, length = 9)
    private String color;

    @Column(nullable = false)
    private int position;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Habit() {}

    public Habit(UUID userId) { this.userId = userId; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public HabitKind getKind() { return kind; }
    public void setKind(HabitKind kind) { this.kind = kind; }
    public InputType getInputType() { return inputType; }
    public void setInputType(InputType inputType) { this.inputType = inputType; }
    public List<String> getOptions() {
        return options == null || options.isEmpty() ? List.of() : Arrays.asList(options.split("\n"));
    }
    public void setOptions(List<String> list) {
        this.options = list == null || list.isEmpty() ? null : String.join("\n", list);
    }
    public boolean isMultiSelect() { return multiSelect; }
    public void setMultiSelect(boolean multiSelect) { this.multiSelect = multiSelect; }
    public int getWeeklyTarget() { return weeklyTarget; }
    public void setWeeklyTarget(int weeklyTarget) { this.weeklyTarget = (short) weeklyTarget; }
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getCreatedAt() { return createdAt; }
}
