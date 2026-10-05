package com.tally.service;

import com.tally.domain.*;
import com.tally.repo.*;
import com.tally.scoring.ScoreEngine;
import com.tally.web.dto.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class LogService {
    private final HabitService habitService;
    private final HabitRepository habits;
    private final EntryRepository entries;
    private final DayMetaRepository days;
    private final AccountService accounts;

    public LogService(HabitService habitService, HabitRepository habits, EntryRepository entries,
                      DayMetaRepository days, AccountService accounts) {
        this.habitService = habitService;
        this.habits = habits;
        this.entries = entries;
        this.days = days;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public WeekResponse week(UUID userId, LocalDate anyDay, LocalDate today) {
        int ws = accounts.get(userId).getWeekStart();
        LocalDate start = ScoreEngine.weekStart(anyDay, ws);
        LocalDate end = start.plusDays(6);

        List<Habit> all = habitService.list(userId);
        List<Entry> weekEntries = entries.findByUserIdAndDayBetween(userId, start, end);
        Set<UUID> withEntries = new HashSet<>();
        weekEntries.forEach(e -> withEntries.add(e.getHabitId()));
        // archived habits still appear in weeks where they were logged, so history stays honest
        List<Habit> shown = all.stream().filter(h -> !h.isArchived() || withEntries.contains(h.getId())).toList();

        Map<LocalDate, DayMeta> meta = new HashMap<>();
        days.findByUserIdAndDayBetween(userId, start, end).forEach(d -> meta.put(d.getDay(), d));
        List<DayDto> dayDtos = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayMeta m = meta.get(d);
            dayDtos.add(new DayDto(d, m != null && m.isRestDay(), m == null ? null : m.getNote()));
        }

        ScoreEngine.WeekScore score = ScoreEngine.scoreWeek(
                shown.stream().map(LogService::info).toList(),
                weekEntries.stream().map(e -> new ScoreEngine.Log(e.getHabitId(), e.getDay())).toList(),
                start, today);

        return new WeekResponse(start, end, today, ws, dayDtos,
                shown.stream().map(HabitDto::of).toList(),
                weekEntries.stream().map(EntryDto::of).toList(),
                score.habits(), score.points(), score.target());
    }

    @Transactional
    public EntryDto upsert(UUID userId, EntryRequest r) {
        Habit h = habitService.get(userId, r.habitId());
        Entry e = entries.findByUserIdAndHabitIdAndDay(userId, h.getId(), r.date())
                .orElseGet(() -> new Entry(userId, h.getId(), r.date()));
        e.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        return EntryDto.of(entries.save(e));
    }

    @Transactional
    public void remove(UUID userId, UUID habitId, LocalDate date) {
        entries.findByUserIdAndHabitIdAndDay(userId, habitId, date).ifPresent(entries::delete);
    }

    @Transactional
    public DayDto setDay(UUID userId, LocalDate date, DayRequest r) {
        DayMeta m = days.findByUserIdAndDay(userId, date).orElseGet(() -> new DayMeta(userId, date));
        if (r.restDay() != null) m.setRestDay(r.restDay());
        if (r.note() != null) m.setNote(r.note().isBlank() ? null : r.note().trim());
        if (!m.isRestDay() && m.getNote() == null) {
            if (days.existsById(m.getId())) days.delete(m);
            return new DayDto(date, false, null);
        }
        days.save(m);
        return new DayDto(date, m.isRestDay(), m.getNote());
    }

    /** Bulk import (e.g. from a Notion CSV export parsed in the browser). Habits are matched by name. */
    @Transactional
    public ImportResult importData(UUID userId, ImportRequest r, LocalDate today) {
        Map<String, Habit> byName = new HashMap<>();
        habitService.list(userId).forEach(h -> byName.put(h.getName().toLowerCase(Locale.ROOT), h));
        LocalDate earliest = r.entries().stream().map(ImportEntry::date).min(LocalDate::compareTo).orElse(today);
        int created = 0;
        for (ImportHabit ih : r.habits()) {
            String k = ih.name().trim().toLowerCase(Locale.ROOT);
            if (byName.containsKey(k)) continue;
            Habit h = habitService.create(userId, new HabitRequest(ih.name().trim(), null,
                    ih.kind() == null ? HabitKind.BUILD : ih.kind(), ih.weeklyTarget(), false, null, earliest, false), today);
            byName.put(k, h);
            created++;
        }
        int n = 0;
        for (ImportEntry ie : r.entries()) {
            Habit h = byName.get(ie.habit().trim().toLowerCase(Locale.ROOT));
            if (h == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown habit in import: " + ie.habit());
            if (ie.date().isBefore(h.getStartDate())) h.setStartDate(ie.date());
            Entry e = entries.findByUserIdAndHabitIdAndDay(userId, h.getId(), ie.date())
                    .orElseGet(() -> new Entry(userId, h.getId(), ie.date()));
            e.setNote(ie.note() == null || ie.note().isBlank() ? null : ie.note().trim());
            entries.save(e);
            n++;
        }
        for (LocalDate d : r.restDays()) setDay(userId, d, new DayRequest(true, null));
        return new ImportResult(created, n, r.restDays().size());
    }

    static ScoreEngine.HabitInfo info(Habit h) {
        return new ScoreEngine.HabitInfo(h.getId(),
                h.getKind() == HabitKind.AVOID ? ScoreEngine.Kind.AVOID : ScoreEngine.Kind.BUILD,
                h.getWeeklyTarget(), h.getStartDate(), h.isArchived());
    }
}
