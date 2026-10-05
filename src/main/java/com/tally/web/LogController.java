package com.tally.web;

import com.tally.security.CurrentUser;
import com.tally.service.LogService;
import com.tally.service.StatsService;
import com.tally.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LogController {
    private final LogService logs;
    private final StatsService stats;

    public LogController(LogService logs, StatsService stats) {
        this.logs = logs;
        this.stats = stats;
    }

    @GetMapping("/week")
    public WeekResponse week(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) String today) {
        LocalDate t = Http.today(today);
        return logs.week(CurrentUser.id(), date == null ? t : date, t);
    }

    @PutMapping("/entries")
    public EntryDto upsert(@Valid @RequestBody EntryRequest r) {
        return logs.upsert(CurrentUser.id(), r);
    }

    @DeleteMapping("/entries")
    public ResponseEntity<Void> remove(@RequestParam UUID habitId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logs.remove(CurrentUser.id(), habitId, date);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/days/{date}")
    public DayDto day(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      @Valid @RequestBody DayRequest r) {
        return logs.setDay(CurrentUser.id(), date, r);
    }

    @GetMapping("/stats")
    public StatsResponse stats(@RequestParam(defaultValue = "12") int weeks, @RequestParam(required = false) String today) {
        return stats.stats(CurrentUser.id(), weeks, Http.today(today));
    }
}
