package com.tally.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple sliding-window limiter for login / PIN attempts (per key, in memory). */
@Component
public class AttemptLimiter {
    private static final int MAX = 10;
    private static final long WINDOW_SECONDS = 15 * 60;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public void check(String key) {
        Deque<Instant> q = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= MAX) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again in a few minutes.");
        }
    }

    public void fail(String key) {
        Deque<Instant> q = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) { q.addLast(Instant.now()); }
    }

    public void reset(String key) { attempts.remove(key); }
}
