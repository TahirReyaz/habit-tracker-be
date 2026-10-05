package com.tally.repo;

import com.tally.domain.WeekTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeekTargetRepository extends JpaRepository<WeekTarget, UUID> {
    List<WeekTarget> findByUserId(UUID userId);
    List<WeekTarget> findByUserIdAndWeekStartBetween(UUID userId, LocalDate from, LocalDate to);
    List<WeekTarget> findByUserIdAndHabitIdAndWeekStartBetween(UUID userId, UUID habitId, LocalDate from, LocalDate to);
}
