package com.tally.service;

import com.tally.domain.Habit;
import com.tally.domain.WeekTarget;
import com.tally.repo.WeekTargetRepository;
import com.tally.scoring.ScoreEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

/**
 * Per-week target overrides ("this week the office target is 4, not 5").
 * Rows are keyed by the week's first day; lookups normalise stored dates to the user's
 * current week-start setting, so overrides survive a Monday/Sunday switch.
 */
@Service
public class WeekTargetService {
    private final WeekTargetRepository repo;
    private final HabitService habits;
    private final AccountService accounts;

    public WeekTargetService(WeekTargetRepository repo, HabitService habits, AccountService accounts) {
        this.repo = repo;
        this.habits = habits;
        this.accounts = accounts;
    }

    /** Overrides for weeks overlapping [from, to]. */
    public ScoreEngine.Targets between(UUID userId, int isoWeekStart, LocalDate from, LocalDate to) {
        return index(repo.findByUserIdAndWeekStartBetween(userId, from.minusDays(6), to), isoWeekStart);
    }

    /** Every override the user has (for statistics over the full history). */
    public ScoreEngine.Targets all(UUID userId, int isoWeekStart) {
        return index(repo.findByUserId(userId), isoWeekStart);
    }

    /** The override map for one week, habit id -> target, for the week view. */
    public Map<UUID, Integer> forWeek(UUID userId, int isoWeekStart, LocalDate weekStart) {
        ScoreEngine.Targets t = between(userId, isoWeekStart, weekStart, weekStart.plusDays(6));
        Map<UUID, Integer> out = new HashMap<>();
        for (Habit h : habits.list(userId)) {
            Integer v = t.forWeek(h.getId(), weekStart);
            if (v != null) out.put(h.getId(), v);
        }
        return out;
    }

    /**
     * Sets the target of one habit for the week containing {@code anyDay}.
     * null (or the habit's usual target) removes the override.
     */
    @Transactional
    public Integer set(UUID userId, UUID habitId, LocalDate anyDay, Integer target) {
        if (target != null && (target < 0 || target > 7))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target must be between 0 and 7");
        Habit h = habits.get(userId, habitId);
        int ws = accounts.get(userId).getWeekStart();
        LocalDate start = ScoreEngine.weekStart(anyDay, ws);
        // remove any row for this week, including ones saved under a different week-start setting
        repo.deleteAll(repo.findByUserIdAndHabitIdAndWeekStartBetween(userId, habitId, start.minusDays(6), start.plusDays(6))
                .stream().filter(w -> ScoreEngine.weekStart(w.getWeekStart(), ws).equals(start)).toList());
        repo.flush();
        if (target == null || target == h.getWeeklyTarget()) return null;
        WeekTarget w = new WeekTarget(userId, habitId, start);
        w.setTarget(target);
        repo.save(w);
        return target;
    }

    private static ScoreEngine.Targets index(List<WeekTarget> rows, int isoWeekStart) {
        if (rows.isEmpty()) return ScoreEngine.Targets.NONE;
        Map<String, Integer> m = new HashMap<>();
        for (WeekTarget w : rows) m.put(key(w.getHabitId(), ScoreEngine.weekStart(w.getWeekStart(), isoWeekStart)), w.getTarget());
        return (habitId, weekStart) -> m.get(key(habitId, weekStart));
    }

    private static String key(UUID habitId, LocalDate weekStart) { return habitId + "|" + weekStart; }
}
