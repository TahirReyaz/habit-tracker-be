package com.tally.scoring;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * All scoring rules in one framework-free class.
 *
 * Rules (mirrors the "0/5" columns of a weekly Notion table):
 *  - BUILD habit: every logged day in the week is 1 point, capped at the weekly target.
 *  - AVOID habit: every elapsed day in the week WITHOUT a logged slip is 1 point, capped at the target.
 *  - Days before a habit's start date never count.
 *  - Weekly total = sum of points / sum of targets of habits that had started by the end of that week.
 */
public final class ScoreEngine {

    public enum Kind { BUILD, AVOID }

    public record HabitInfo(UUID id, Kind kind, int target, LocalDate startDate, boolean archived) {}
    public record Log(UUID habitId, LocalDate day) {}

    public record HabitWeek(UUID habitId, int count, int points, int target, boolean started) {}
    public record WeekScore(LocalDate start, int points, int target, List<HabitWeek> habits) {
        public double pct() { return target == 0 ? 0 : (double) points / target; }
    }

    public record HabitStats(UUID habitId, int totalDays, int weeksCounted, int weeksHit, double hitRate,
                             double avgPerWeek, int currentWeekStreak, int longestWeekStreak,
                             int currentDayStreak, int longestDayStreak, int[] weekday, int[] weekly) {}

    public record DayCell(LocalDate date, int done, int possible, int slips, boolean rest) {}

    public record Stats(LocalDate from, LocalDate to, List<WeekScore> weeks, List<HabitStats> habits, List<DayCell> days,
                        int thisWeekPoints, int thisWeekTarget, double avgPct, LocalDate bestWeekStart, double bestWeekPct,
                        int totalLogs, int restDays, int perfectWeeks) {}

    private ScoreEngine() {}

    public static LocalDate weekStart(LocalDate date, int isoWeekStart) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(isoWeekStart)));
    }

    /** Score one week. {@code logs} may contain logs outside the week; they are ignored. */
    public static WeekScore scoreWeek(List<HabitInfo> habits, Collection<Log> logs, LocalDate weekStart, LocalDate today) {
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<UUID, Set<LocalDate>> byHabit = index(logs, weekStart, weekEnd);
        List<HabitWeek> out = new ArrayList<>();
        int pts = 0, tgt = 0;
        for (HabitInfo h : habits) {
            Set<LocalDate> days = byHabit.getOrDefault(h.id(), Set.of());
            boolean started = !h.startDate().isAfter(weekEnd);
            if (h.archived() && days.isEmpty()) continue;
            int count = 0, points = 0;
            if (started) {
                if (h.kind() == Kind.BUILD) {
                    count = (int) days.stream().filter(d -> !d.isBefore(h.startDate())).count();
                    points = Math.min(count, h.target());
                } else {
                    int slips = 0, eligible = 0;
                    for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                        if (d.isBefore(h.startDate()) || d.isAfter(today)) continue;
                        eligible++;
                        if (days.contains(d)) slips++;
                    }
                    count = slips;
                    points = Math.min(eligible - slips, h.target());
                }
                pts += points;
                tgt += h.target();
            }
            out.add(new HabitWeek(h.id(), count, points, h.target(), started));
        }
        return new WeekScore(weekStart, pts, tgt, out);
    }

    /**
     * Statistics over the {@code nWeeks} weeks ending with the week containing {@code today}.
     * {@code allLogs} should be the user's full history so streaks are not truncated by the range.
     */
    public static Stats stats(List<HabitInfo> habits, List<Log> allLogs, Set<LocalDate> restDays,
                              int nWeeks, LocalDate today, int isoWeekStart) {
        LocalDate thisWeek = weekStart(today, isoWeekStart);
        LocalDate from = thisWeek.minusWeeks(nWeeks - 1L);
        LocalDate to = thisWeek.plusDays(6);

        List<WeekScore> weeks = new ArrayList<>();
        for (int i = 0; i < nWeeks; i++) weeks.add(scoreWeek(habits, allLogs, from.plusWeeks(i), today));

        Map<UUID, Set<LocalDate>> all = index(allLogs, LocalDate.MIN, LocalDate.MAX);

        List<HabitStats> hs = new ArrayList<>();
        for (HabitInfo h : habits) {
            Set<LocalDate> days = all.getOrDefault(h.id(), Set.of());
            int[] weekday = new int[7];
            int[] weekly = new int[nWeeks];
            int weeksCounted = 0, weeksHit = 0, totalDays = 0, completedPoints = 0;
            for (int i = 0; i < nWeeks; i++) {
                WeekScore ws = weeks.get(i);
                HabitWeek hw = find(ws, h.id());
                if (hw == null || !hw.started()) { weekly[i] = 0; continue; }
                weekly[i] = hw.points();
                boolean completed = ws.start().plusDays(6).isBefore(today);
                boolean hit = hw.points() >= hw.target();
                if (completed || hit) {
                    weeksCounted++;
                    completedPoints += hw.points();
                    if (hit) weeksHit++;
                }
            }
            for (LocalDate d : days) {
                if (d.isBefore(from) || d.isAfter(to) || d.isBefore(h.startDate())) continue;
                weekday[d.getDayOfWeek().getValue() - 1]++;
                if (h.kind() == Kind.BUILD) totalDays++;
            }
            if (h.kind() == Kind.AVOID) {
                for (LocalDate d = max(from, h.startDate()); !d.isAfter(min(to, today)); d = d.plusDays(1)) {
                    if (!days.contains(d)) totalDays++;
                }
            }

            // week streaks walk the full history, not just the range
            int[] weekStreaks = weekStreaks(h, allLogs, today, isoWeekStart);
            int[] dayStreaks = dayStreaks(h, days, today);

            double hitRate = weeksCounted == 0 ? 0 : (double) weeksHit / weeksCounted;
            double avg = weeksCounted == 0 ? 0 : (double) completedPoints / weeksCounted;
            hs.add(new HabitStats(h.id(), totalDays, weeksCounted, weeksHit, hitRate, avg,
                    weekStreaks[0], weekStreaks[1], dayStreaks[0], dayStreaks[1], weekday, weekly));
        }

        // daily heatmap
        List<DayCell> cells = new ArrayList<>();
        Map<LocalDate, List<UUID>> logsByDay = new HashMap<>();
        for (Log l : allLogs) if (!l.day().isBefore(from) && !l.day().isAfter(to))
            logsByDay.computeIfAbsent(l.day(), k -> new ArrayList<>()).add(l.habitId());
        Map<UUID, HabitInfo> hById = new HashMap<>();
        habits.forEach(h -> hById.put(h.id(), h));
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            int possible = (int) habits.stream().filter(h -> h.kind() == Kind.BUILD && !h.archived() && !h.startDate().isAfter(day)).count();
            int done = 0, slips = 0;
            for (UUID id : logsByDay.getOrDefault(d, List.of())) {
                HabitInfo h = hById.get(id);
                if (h == null || d.isBefore(h.startDate())) continue;
                if (h.kind() == Kind.BUILD) done++; else slips++;
            }
            cells.add(new DayCell(d, done, possible, slips, restDays.contains(d)));
        }

        WeekScore current = weeks.get(weeks.size() - 1);
        double sumPct = 0; int nCompleted = 0, perfect = 0;
        LocalDate best = null; double bestPct = -1;
        for (WeekScore ws : weeks) {
            if (ws.target() == 0) continue;
            boolean completed = ws.start().plusDays(6).isBefore(today);
            if (completed) { sumPct += ws.pct(); nCompleted++; }
            if (ws.points() >= ws.target()) perfect++;
            if (ws.pct() > bestPct) { bestPct = ws.pct(); best = ws.start(); }
        }
        int totalLogs = (int) allLogs.stream().filter(l -> !l.day().isBefore(from) && !l.day().isAfter(to)).count();
        int rest = (int) restDays.stream().filter(d -> !d.isBefore(from) && !d.isAfter(to)).count();

        return new Stats(from, to, weeks, hs, cells, current.points(), current.target(),
                nCompleted == 0 ? 0 : sumPct / nCompleted, best, Math.max(bestPct, 0), totalLogs, rest, perfect);
    }

    /** [current, longest] count of consecutive weeks where the weekly target was met. */
    static int[] weekStreaks(HabitInfo h, List<Log> allLogs, LocalDate today, int isoWeekStart) {
        LocalDate first = weekStart(h.startDate(), isoWeekStart);
        LocalDate thisWeek = weekStart(today, isoWeekStart);
        if (first.isAfter(thisWeek)) return new int[]{0, 0};
        List<HabitInfo> one = List.of(new HabitInfo(h.id(), h.kind(), h.target(), h.startDate(), false));
        List<Log> mine = allLogs.stream().filter(l -> l.habitId().equals(h.id())).toList();
        int longest = 0, run = 0;
        List<Boolean> hits = new ArrayList<>();
        for (LocalDate w = first; !w.isAfter(thisWeek); w = w.plusWeeks(1)) {
            HabitWeek hw = scoreWeek(one, mine, w, today).habits().get(0);
            boolean hit = hw.points() >= hw.target();
            hits.add(hit);
            run = hit ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        // current: count back from this week; an unfinished current week doesn't break the streak
        int current = 0;
        int i = hits.size() - 1;
        if (!hits.get(i)) i--;
        for (; i >= 0 && hits.get(i); i--) current++;
        return new int[]{current, longest};
    }

    /** [current, longest] consecutive days: logged days for BUILD, clean days for AVOID. */
    static int[] dayStreaks(HabitInfo h, Set<LocalDate> days, LocalDate today) {
        if (h.startDate().isAfter(today)) return new int[]{0, 0};
        if (h.kind() == Kind.BUILD) {
            List<LocalDate> sorted = days.stream().filter(d -> !d.isBefore(h.startDate()) && !d.isAfter(today)).sorted().toList();
            int longest = 0, run = 0; LocalDate prev = null;
            for (LocalDate d : sorted) {
                run = (prev != null && ChronoUnit.DAYS.between(prev, d) == 1) ? run + 1 : 1;
                longest = Math.max(longest, run);
                prev = d;
            }
            int current = 0;
            LocalDate d = days.contains(today) ? today : today.minusDays(1);
            while (days.contains(d) && !d.isBefore(h.startDate())) { current++; d = d.minusDays(1); }
            return new int[]{current, longest};
        } else {
            int longest = 0, run = 0;
            for (LocalDate d = h.startDate(); !d.isAfter(today); d = d.plusDays(1)) {
                run = days.contains(d) ? 0 : run + 1;
                longest = Math.max(longest, run);
            }
            return new int[]{run, longest};
        }
    }

    private static HabitWeek find(WeekScore ws, UUID id) {
        for (HabitWeek hw : ws.habits()) if (hw.habitId().equals(id)) return hw;
        return null;
    }

    private static Map<UUID, Set<LocalDate>> index(Collection<Log> logs, LocalDate from, LocalDate to) {
        Map<UUID, Set<LocalDate>> m = new HashMap<>();
        for (Log l : logs) {
            if (l.day().isBefore(from) || l.day().isAfter(to)) continue;
            m.computeIfAbsent(l.habitId(), k -> new HashSet<>()).add(l.day());
        }
        return m;
    }

    private static LocalDate max(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate min(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }
}
