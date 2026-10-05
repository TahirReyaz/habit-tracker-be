package com.tally.service;

import com.tally.domain.DayMeta;
import com.tally.domain.Habit;
import com.tally.repo.DayMetaRepository;
import com.tally.repo.EntryRepository;
import com.tally.scoring.ScoreEngine;
import com.tally.web.dto.Dtos.HabitDto;
import com.tally.web.dto.Dtos.StatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final HabitService habits;
    private final EntryRepository entries;
    private final DayMetaRepository days;
    private final AccountService accounts;

    public StatsService(HabitService habits, EntryRepository entries, DayMetaRepository days, AccountService accounts) {
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
        ScoreEngine.Stats s = ScoreEngine.stats(hs.stream().map(LogService::info).toList(), logs, rest, n, today, ws);
        return new StatsResponse(hs.stream().map(HabitDto::of).toList(), s);
    }
}
