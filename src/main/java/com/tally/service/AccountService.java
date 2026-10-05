package com.tally.service;

import com.tally.domain.User;
import com.tally.repo.*;
import com.tally.security.AttemptLimiter;
import com.tally.web.dto.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private final UserRepository users;
    private final HabitRepository habits;
    private final EntryRepository entries;
    private final DayMetaRepository days;
    private final WeekTargetRepository weekTargets;
    private final PasswordEncoder encoder;
    private final AttemptLimiter limiter;

    public AccountService(UserRepository users, HabitRepository habits, EntryRepository entries,
                          DayMetaRepository days, WeekTargetRepository weekTargets,
                          PasswordEncoder encoder, AttemptLimiter limiter) {
        this.weekTargets = weekTargets;
        this.users = users;
        this.habits = habits;
        this.entries = entries;
        this.days = days;
        this.encoder = encoder;
        this.limiter = limiter;
    }

    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @Transactional
    public User updateSettings(UUID id, SettingsRequest r) {
        User u = get(id);
        if (r.weekStart() != null) u.setWeekStart(r.weekStart());
        if (r.lockMinutes() != null) u.setLockMinutes(r.lockMinutes());
        if (r.discreetDefault() != null) u.setDiscreetDefault(r.discreetDefault());
        return u;
    }

    @Transactional
    public User setPin(UUID id, PinRequest r) {
        User u = get(id);
        requirePassword(u, r.password());
        u.setLockPinHash(r.pin() == null || r.pin().isBlank() ? null : encoder.encode(r.pin()));
        if (u.getLockPinHash() == null) u.setLockMinutes(0);
        return u;
    }

    public boolean unlock(UUID id, String pin) {
        User u = get(id);
        if (u.getLockPinHash() == null) return true;
        String key = "pin:" + id;
        limiter.check(key);
        boolean ok = encoder.matches(pin, u.getLockPinHash());
        if (ok) limiter.reset(key); else limiter.fail(key);
        return ok;
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest r) {
        User u = get(id);
        requirePassword(u, r.current());
        u.setPasswordHash(encoder.encode(r.next()));
    }

    @Transactional
    public void delete(UUID id, String password) {
        User u = get(id);
        requirePassword(u, password);
        users.delete(u); // habits, entries, day_meta cascade in the database
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID id) {
        User u = get(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", Instant.now());
        out.put("account", MeResponse.of(u));
        out.put("habits", habits.findByUserIdOrderByPositionAscCreatedAtAsc(id).stream().map(HabitDto::of).toList());
        out.put("entries", entries.findByUserIdOrderByDayAsc(id).stream().map(EntryDto::of).toList());
        out.put("weekTargets", weekTargets.findByUserId(id).stream()
                .map(w -> Map.of("habitId", w.getHabitId(), "weekStart", w.getWeekStart(), "target", w.getTarget())).toList());
        out.put("days", days.findByUserIdOrderByDayAsc(id).stream()
                .map(d -> new DayDto(d.getDay(), d.isRestDay(), d.getNote())).toList());
        return out;
    }

    private void requirePassword(User u, String password) {
        String key = "pw:" + u.getId();
        limiter.check(key);
        if (password == null || !encoder.matches(password, u.getPasswordHash())) {
            limiter.fail(key);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is incorrect");
        }
        limiter.reset(key);
    }
}
