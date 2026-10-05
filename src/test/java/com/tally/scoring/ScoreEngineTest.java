package com.tally.scoring;

import com.tally.scoring.ScoreEngine.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScoreEngineTest {
    final LocalDate today = LocalDate.of(2026, 10, 7); // Wednesday
    final LocalDate wk = ScoreEngine.weekStart(today, 1);
    final UUID build = UUID.randomUUID(), avoid = UUID.randomUUID(), future = UUID.randomUUID();
    final LocalDate start = LocalDate.of(2026, 9, 1);
    final List<HabitInfo> habits = List.of(
            new HabitInfo(build, Kind.BUILD, 5, start, false),
            new HabitInfo(avoid, Kind.AVOID, 7, start, false),
            new HabitInfo(future, Kind.BUILD, 3, LocalDate.of(2026, 12, 1), false));

    @Test
    void weekStartRespectsSetting() {
        assertEquals(LocalDate.of(2026, 10, 5), ScoreEngine.weekStart(today, 1));
        assertEquals(LocalDate.of(2026, 10, 4), ScoreEngine.weekStart(today, 7));
    }

    @Test
    void scoresBuildAndAvoidHabits() {
        List<Log> logs = List.of(new Log(build, wk), new Log(build, wk.plusDays(1)), new Log(avoid, wk.plusDays(1)));
        WeekScore ws = ScoreEngine.scoreWeek(habits, logs, wk, today);
        assertEquals(2, ws.habits().get(0).points());
        assertEquals(2, ws.habits().get(1).points(), "3 elapsed days minus 1 slip");
        assertEquals(12, ws.target(), "habit that has not started is excluded");
        assertEquals(4, ws.points());
    }

    @Test
    void capsAtTargetAndTracksStreaks() {
        List<Log> logs = new ArrayList<>(List.of(new Log(build, wk), new Log(build, wk.plusDays(1)), new Log(avoid, wk.plusDays(1))));
        for (int w = 1; w <= 2; w++) for (int d = 0; d < 6; d++) logs.add(new Log(build, wk.minusWeeks(w).plusDays(d)));
        Stats st = ScoreEngine.stats(habits, logs, Set.of(wk), 4, today, 1);
        assertEquals(5, st.weeks().get(2).habits().get(0).points());
        HabitStats b = st.habits().get(0);
        assertEquals(2, b.currentWeekStreak());
        assertEquals(2, b.longestWeekStreak());
        assertEquals(2, b.currentDayStreak(), "streak continues while today is still open");
        assertEquals(1, st.habits().get(1).currentDayStreak(), "clean days since last slip");
        assertEquals(1, st.days().stream().filter(DayCell::rest).count());
    }
}
