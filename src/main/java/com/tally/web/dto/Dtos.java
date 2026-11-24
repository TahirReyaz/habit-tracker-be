package com.tally.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tally.domain.*;
import com.tally.scoring.ScoreEngine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All request/response shapes, kept together so the API surface is readable in one place. */
public final class Dtos {
    private Dtos() {}

    // ---- auth / account ----
    public record AuthRequest(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(min = 8, max = 128) String password) {}

    public record MeResponse(UUID id, String email, int weekStart, int lockMinutes,
                             boolean discreetDefault, boolean hasPin, Instant createdAt) {
        public static MeResponse of(User u) {
            return new MeResponse(u.getId(), u.getEmail(), u.getWeekStart(), u.getLockMinutes(),
                    u.isDiscreetDefault(), u.getLockPinHash() != null, u.getCreatedAt());
        }
    }

    public record SettingsRequest(@Min(1) @Max(7) Integer weekStart,
                                  @Min(0) @Max(240) Integer lockMinutes,
                                  Boolean discreetDefault) {}

    public record PinRequest(@Pattern(regexp = "^\\d{4,8}$", message = "PIN must be 4-8 digits") String pin,
                             @NotBlank String password) {}

    public record UnlockRequest(@NotBlank String pin) {}

    public record PasswordRequest(@NotBlank String password) {}

    public record ChangePasswordRequest(@NotBlank String current, @NotBlank @Size(min = 8, max = 128) String next) {}

    // ---- habits ----
    /** limitMin / limitMax / unit only mean something for LIMIT habits (null = no bound). */
    public record HabitDto(UUID id, String name, String alias, HabitKind kind, InputType inputType, List<String> options,
                           boolean multiSelect, int weeklyTarget, @JsonProperty("isPrivate") boolean isPrivate, String color,
                           int position, LocalDate startDate, boolean archived,
                           Double limitMin, Double limitMax, String unit) {
        public static HabitDto of(Habit h) {
            return new HabitDto(h.getId(), h.getName(), h.getAlias(), h.getKind(), h.getInputType(), h.getOptions(),
                    h.isMultiSelect(), h.getWeeklyTarget(), h.isPrivate(), h.getColor(), h.getPosition(), h.getStartDate(),
                    h.isArchived(), h.getLimitMin(), h.getLimitMax(), h.getUnit());
        }
    }

    /**
     * Used for create (name + weeklyTarget required, except for LIMIT habits) and patch (all optional).
     * optionRenames (old label -> new label) rewrites past entries of a SELECT habit so history follows a rename.
     * setLimits = true applies limitMin / limitMax / unit exactly as sent (null clears a bound); on create they always apply.
     */
    public record HabitRequest(@Size(min = 1, max = 80) String name,
                               @Size(max = 40) String alias,
                               HabitKind kind,
                               InputType inputType,
                               @Size(max = 50) List<@Size(max = 60) String> options,
                               Map<String, String> optionRenames,
                               Boolean multiSelect,
                               @Min(1) @Max(7) Integer weeklyTarget,
                               @JsonProperty("isPrivate") Boolean isPrivate,
                               @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color,
                               LocalDate startDate,
                               Boolean archived,
                               Double limitMin,
                               Double limitMax,
                               @Size(max = 16) String unit,
                               Boolean setLimits) {}

    public record ReorderRequest(@NotNull List<UUID> ids) {}

    // ---- logging ----
    /**
     * value: the text (TEXT), a single option (SELECT) or the amount (LIMIT). values: the picked options
     * (SELECT, several when multi-select). Both ignored for CHECK. note: optional extra for CHECK/SELECT/LIMIT.
     */
    public record EntryRequest(@NotNull UUID habitId, @NotNull LocalDate date,
                               @Size(max = 2000) String value,
                               @Size(max = 50) List<@Size(max = 60) String> values,
                               @Size(max = 2000) String note) {}

    /**
     * value: raw stored value (TEXT text; SELECT options newline-joined; LIMIT amount). values: SELECT options as a list.
     * penalty: LIMIT only — the amount broke the habit's limit (a −1 mark).
     */
    public record EntryDto(UUID habitId, LocalDate date, String value, List<String> values, String note, boolean penalty) {
        public static EntryDto of(Entry e) {
            return new EntryDto(e.getHabitId(), e.getDay(), e.getValue(), e.getValues(), e.getNote(), e.isPenalty());
        }
    }

    /**
     * EntryDto plus the habit fields that logging can change: options (new ones created while logging)
     * and startDate (moved back when a day before it is logged, so back-filled weeks count).
     */
    public record LogResult(EntryDto entry, List<String> options, LocalDate startDate) {}

    public record DayRequest(Boolean restDay, @Size(max = 2000) String note) {}

    public record DayDto(LocalDate date, boolean restDay, String note) {}

    public record WeekResponse(LocalDate start, LocalDate end, LocalDate today, int weekStart,
                               List<DayDto> days, List<HabitDto> habits, List<EntryDto> entries,
                               List<ScoreEngine.HabitWeek> scores, int points, int target,
                               Map<UUID, Integer> targets) {}

    /** target null = back to the habit's usual weekly target. date = any day of the week. */
    public record WeekTargetRequest(@NotNull UUID habitId, @NotNull LocalDate date, @Min(0) @Max(7) Integer target) {}

    /** Summary of the logged amounts of one LIMIT habit in the stats range. */
    public record LimitStat(int days, Double avg, Double lowest, Double highest, int over, int under) {}

    /**
     * optionCounts: SELECT habit id -> option label -> number of days in range.
     * limitStats: LIMIT habit id -> summary of logged amounts in range.
     */
    public record StatsResponse(List<HabitDto> habits, ScoreEngine.Stats stats,
                                Map<UUID, Map<String, Integer>> optionCounts,
                                Map<UUID, LimitStat> limitStats) {}

    // ---- import ----
    public record ImportHabit(@NotBlank @Size(max = 80) String name, @Min(1) @Max(7) int weeklyTarget, HabitKind kind, InputType inputType) {}
    /** text: the cell content — stored as the value for TEXT/SELECT habits and as the note for CHECK habits. */
    public record ImportEntry(@NotBlank String habit, @NotNull LocalDate date, @Size(max = 2000) String text) {}
    public record ImportRequest(@NotNull @Size(max = 50) List<@Valid ImportHabit> habits,
                                @NotNull @Size(max = 20000) List<@Valid ImportEntry> entries,
                                @NotNull List<LocalDate> restDays) {}
    public record ImportResult(int habitsCreated, int entriesImported, int restDays) {}
}
