package com.tally.service;

import com.tally.domain.DayMeta;
import com.tally.domain.Entry;
import com.tally.domain.Habit;
import com.tally.domain.InputType;
import com.tally.repo.DayMetaRepository;
import com.tally.repo.EntryRepository;
import com.tally.scoring.ScoreEngine;
import com.tally.web.dto.Dtos.HabitDto;
import com.tally.web.dto.Dtos.LimitStat;
import com.tally.web.dto.Dtos.StatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final HabitService habits;
    private final EntryRepository entries;
    private final DayMetaRepository days;
    private final AccountService accounts;
    private final WeekTargetService weekTargets;

    public StatsService(HabitService habits, EntryRepository entries, DayMetaRepository days, AccountService accounts,
                        WeekTargetService weekTargets) {
        this.weekTargets = weekTargets;
        this.habits = habits;
        this.entries = entries;
        this.days = days;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(UUID userId, int weeks, LocalDate today) {
        int n = Math.max(1, Math.min(weeks, 104));
        int ws = accounts.get(userId).getWeekStart();
        List<Habit> hs = habits.list(userId);
        List<ScoreEngine.Log> logs = entries.findKeys(userId).stream()
                .map(r -> new ScoreEngine.Log((UUID) r[0], (LocalDate) r[1], Boolean.TRUE.equals(r[2]))).toList();
        Set<LocalDate> rest = days.findByUserIdOrderByDayAsc(userId).stream()
                .filter(DayMeta::isRestDay).map(DayMeta::getDay).collect(Collectors.toSet());
        ScoreEngine.Stats s = ScoreEngine.stats(hs.stream().map(LogService::info).toList(), logs, rest, n, today, ws,
                weekTargets.all(userId, ws));
        List<Entry> inRange = needsValues(hs) ? entries.findByUserIdAndDayBetween(userId, s.from(), s.to()) : List.of();
        return new StatsResponse(hs.stream().map(HabitDto::of).toList(), s,
                optionCounts(hs, inRange), limitStats(hs, inRange));
    }

    private static boolean needsValues(List<Habit> hs) {
        return hs.stream().anyMatch(h -> h.getInputType() == InputType.SELECT || h.getInputType() == InputType.LIMIT);
    }

    /** Average / lowest / highest logged amount and days over / under the range, per LIMIT habit. */
    private Map<UUID, LimitStat> limitStats(List<Habit> hs, List<Entry> inRange) {
        Map<UUID, Habit> limit = new HashMap<>();
        hs.stream().filter(h -> h.getInputType() == InputType.LIMIT).forEach(h -> limit.put(h.getId(), h));
        Map<UUID, List<Double>> amounts = new HashMap<>();
        for (Entry e : inRange) {
            Habit h = limit.get(e.getHabitId());
            if (h == null || e.getValue() == null || e.getDay().isBefore(h.getStartDate())) continue;
            try {
                amounts.computeIfAbsent(h.getId(), k -> new ArrayList<>()).add(Double.parseDouble(e.getValue()));
            } catch (NumberFormatException ignored) {
                // a non-numeric value (e.g. the habit used to be TEXT) carries no amount
            }
        }
        Map<UUID, LimitStat> out = new HashMap<>();
        amounts.forEach((id, list) -> {
            Habit h = limit.get(id);
            DoubleSummaryStatistics st = list.stream().mapToDouble(Double::doubleValue).summaryStatistics();
            int over = (int) list.stream().filter(v -> h.getLimitMax() != null && v > h.getLimitMax()).count();
            int under = (int) list.stream().filter(v -> h.getLimitMin() != null && v < h.getLimitMin()).count();
            out.put(id, new LimitStat(list.size(), round(st.getAverage()), st.getMin(), st.getMax(), over, under));
        });
        return out;
    }

    private static double round(double d) { return Math.round(d * 100) / 100.0; }

    /** How often each option was picked in range, per SELECT habit (most used first). */
    private Map<UUID, Map<String, Integer>> optionCounts(List<Habit> hs, List<Entry> inRange) {
        Set<UUID> select = hs.stream().filter(h -> h.getInputType() == InputType.SELECT).map(Habit::getId).collect(Collectors.toSet());
        Map<UUID, Map<String, Integer>> out = new HashMap<>();
        if (select.isEmpty()) return out;
        for (Entry e : inRange) {
            if (!select.contains(e.getHabitId()) || e.getValue() == null) continue;
            Map<String, Integer> m = out.computeIfAbsent(e.getHabitId(), k -> new HashMap<>());
            for (String v : e.getValues()) m.merge(v, 1, Integer::sum); // multi-select: each pick counts

        }
        Map<UUID, Map<String, Integer>> sorted = new HashMap<>();
        out.forEach((id, m) -> {
            Map<String, Integer> lm = new LinkedHashMap<>();
            m.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(x -> lm.put(x.getKey(), x.getValue()));
            sorted.put(id, lm);
        });
        return sorted;
    }
}
