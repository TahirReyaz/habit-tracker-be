package com.tally.web;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;

final class Http {
    private Http() {}

    /** The client sends its local date so "today" follows the user's timezone, not the server's. */
    static LocalDate today(String clientToday) {
        if (clientToday == null || clientToday.isBlank()) return LocalDate.now();
        LocalDate d = LocalDate.parse(clientToday);
        LocalDate server = LocalDate.now();
        // clamp to +-1 day of the server clock (timezones span ~26h)
        if (d.isAfter(server.plusDays(1))) return server.plusDays(1);
        if (d.isBefore(server.minusDays(1))) return server.minusDays(1);
        return d;
    }

    static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
