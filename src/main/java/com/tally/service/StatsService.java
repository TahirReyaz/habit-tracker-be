package com.tally.service;

import com.tally.domain.DayMeta;
import com.tally.domain.Entry;
import com.tally.domain.Habit;
import com.tally.domain.InputType;
import com.tally.repo.DayMetaRepository;
import com.tally.repo.EntryRepository;
import com.tally.scoring.ScoreEngine;
import com.tally.web.dto.Dtos.HabitDto;
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
                .map(r -> new ScoreEngine.Log((UUID) r[0], (LocalDate) r[1])).toList();
        Set<LocalDate> rest = days.findByUserIdOrderByDayAsc(userId).stream()
                .filter(DayMeta::isRestDay).map(DayMeta::getDay).collect(Collectors.toSet());
        ScoreEngine.Stats s = ScoreEngine.stats(hs.stream().map(LogService::info).toList(), logs, rest, n, today, ws,
                weekTargets.all(userId, ws));
        return new StatsResponse(hs.stream().map(HabitDto::of).toList(), s, optionCounts(userId, hs, s.from(), s.to()));
    }

    /** How often each option was picked in range, per SELECT habit (most used first). */
    private Map<UUID, Map<String, Integer>> optionCounts(UUID userId, List<Habit> hs, LocalDate from, LocalDate to) {
        Set<UUID> select = hs.stream().filter(h -> h.getInputType() == InputType.SELECT).map(Habit::getId).collect(Collectors.toSet());
        Map<UUID, Map<String, Integer>> out = new HashMap<>();
        if (select.isEmpty()) return out;
        for (Entry e : entries.findByUserIdAndDayBetween(userId, from, to)) {
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
