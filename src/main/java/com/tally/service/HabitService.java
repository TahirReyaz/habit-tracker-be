package com.tally.service;

import com.tally.domain.Entry;
import com.tally.domain.Habit;
import com.tally.domain.HabitKind;
import com.tally.domain.InputType;
import com.tally.repo.EntryRepository;
import com.tally.repo.HabitRepository;
import com.tally.web.dto.Dtos.HabitRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class HabitService {
    public static final List<String> PALETTE = List.of(
            "#3987e5", "#d95926", "#199e70", "#c98500", "#d55181", "#008300", "#9085e9", "#e66767");
    private static final int MAX_HABITS = 40;
    public static final int MAX_OPTIONS = 50;
    public static final int MAX_OPTION_LENGTH = 60;

    private final HabitRepository habits;
    private final EntryRepository entries;

    public HabitService(HabitRepository habits, EntryRepository entries) {
        this.habits = habits;
        this.entries = entries;
    }

    public List<Habit> list(UUID userId) {
        return habits.findByUserIdOrderByPositionAscCreatedAtAsc(userId);
    }

    public Habit get(UUID userId, UUID id) {
        return habits.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFound("Habit"));
    }

    @Transactional
    public Habit create(UUID userId, HabitRequest r, LocalDate today) {
        if (r.name() == null || r.name().isBlank()) throw bad("Name is required");
        if (r.weeklyTarget() == null) throw bad("Weekly target is required");
        List<Habit> existing = list(userId);
        if (existing.size() >= MAX_HABITS) throw bad("Habit limit reached");
        Habit h = new Habit(userId);
        h.setName(r.name().trim());
        h.setWeeklyTarget(r.weeklyTarget());
        h.setKind(r.kind() == null ? HabitKind.BUILD : r.kind());
        h.setInputType(r.inputType() == null ? InputType.CHECK : r.inputType());
        h.setOptions(cleanOptions(r.options()));
        h.setMultiSelect(Boolean.TRUE.equals(r.multiSelect()));
        h.setPrivate(Boolean.TRUE.equals(r.isPrivate()));
        h.setAlias(blank(r.alias()) ? nextAlias(existing) : r.alias().trim());
        h.setColor(r.color() == null ? PALETTE.get(existing.size() % PALETTE.size()) : r.color());
        h.setStartDate(r.startDate() == null ? today : r.startDate());
        h.setPosition(existing.stream().mapToInt(Habit::getPosition).max().orElse(-1) + 1);
        return habits.save(h);
    }

    @Transactional
    public Habit update(UUID userId, UUID id, HabitRequest r) {
        Habit h = get(userId, id);
        if (r.name() != null) { if (r.name().isBlank()) throw bad("Name is required"); h.setName(r.name().trim()); }
        if (r.alias() != null) h.setAlias(r.alias().isBlank() ? nextAlias(list(userId)) : r.alias().trim());
        if (r.kind() != null) h.setKind(r.kind());
        if (r.inputType() != null) h.setInputType(r.inputType());
        if (r.options() != null) h.setOptions(cleanOptions(r.options()));
        if (r.multiSelect() != null) h.setMultiSelect(r.multiSelect());
        if (r.optionRenames() != null && !r.optionRenames().isEmpty()) applyRenames(userId, h, r.optionRenames());
        if (r.weeklyTarget() != null) h.setWeeklyTarget(r.weeklyTarget());
        if (r.isPrivate() != null) h.setPrivate(r.isPrivate());
        if (r.color() != null) h.setColor(r.color());
        if (r.startDate() != null) h.setStartDate(r.startDate());
        if (r.archived() != null) h.setArchived(r.archived());
        return h;
    }

    /** Adds an option to a SELECT habit if it isn't there yet (used when an option is created while logging). */
    @Transactional
    public void ensureOption(Habit h, String option) {
        List<String> opts = new ArrayList<>(h.getOptions());
        if (opts.stream().anyMatch(o -> o.equalsIgnoreCase(option))) return;
        if (opts.size() >= MAX_OPTIONS) throw bad("Option limit reached (" + MAX_OPTIONS + ")");
        opts.add(option);
        h.setOptions(opts);
    }

    /** Returns the existing label matching case-insensitively, so "gym" and "Gym" don't become two options. */
    public String canonicalOption(Habit h, String option) {
        for (String o : h.getOptions()) if (o.equalsIgnoreCase(option)) return o;
        return option;
    }

    @Transactional
    public void reorder(UUID userId, List<UUID> ids) {
        Map<UUID, Habit> mine = new HashMap<>();
        list(userId).forEach(h -> mine.put(h.getId(), h));
        int pos = 0;
        for (UUID id : ids) {
            Habit h = mine.remove(id);
            if (h != null) h.setPosition(pos++);
        }
        for (Habit rest : mine.values()) rest.setPosition(pos++);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        habits.delete(get(userId, id)); // entries cascade
    }

    /** Values are encrypted, so renames are applied in Java rather than with an UPDATE ... WHERE value = ?. */
    private void applyRenames(UUID userId, Habit h, Map<String, String> renames) {
        Map<String, String> clean = new HashMap<>();
        renames.forEach((from, to) -> {
            if (from != null && to != null && !to.isBlank() && !from.equals(to.trim())) clean.put(from, cleanOption(to));
        });
        if (clean.isEmpty()) return;
        for (Entry e : entries.findByUserIdAndHabitId(userId, h.getId())) {
            if (e.getValue() == null) continue;
            List<String> vals = e.getValues();
            if (vals.stream().noneMatch(clean::containsKey)) continue;
            LinkedHashSet<String> next = new LinkedHashSet<>();
            for (String v : vals) next.add(clean.getOrDefault(v, v));
            e.setValue(String.join("\n", next));
        }
    }

    public static String cleanOption(String raw) {
        String s = raw.replaceAll("[\\r\\n]+", " ").trim();
        if (s.isEmpty()) throw bad("Option can't be empty");
        if (s.length() > MAX_OPTION_LENGTH) throw bad("Options are limited to " + MAX_OPTION_LENGTH + " characters");
        return s;
    }

    static List<String> cleanOptions(List<String> raw) {
        if (raw == null) return List.of();
        LinkedHashMap<String, String> seen = new LinkedHashMap<>();
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            String s = cleanOption(r);
            seen.putIfAbsent(s.toLowerCase(Locale.ROOT), s);
        }
        if (seen.size() > MAX_OPTIONS) throw bad("Up to " + MAX_OPTIONS + " options");
        return new ArrayList<>(seen.values());
    }

    /** Neutral placeholder names: "Habit A", "Habit B", ... */
    static String nextAlias(List<Habit> existing) {
        Set<String> used = new HashSet<>();
        existing.forEach(h -> used.add(h.getAlias()));
        for (int i = 0; ; i++) {
            String a = "Habit " + label(i);
            if (!used.contains(a)) return a;
        }
    }

    private static String label(int i) {
        StringBuilder sb = new StringBuilder();
        do { sb.insert(0, (char) ('A' + i % 26)); i = i / 26 - 1; } while (i >= 0);
        return sb.toString();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
