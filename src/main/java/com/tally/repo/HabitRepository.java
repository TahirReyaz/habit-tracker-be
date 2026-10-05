package com.tally.repo;

import com.tally.domain.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitRepository extends JpaRepository<Habit, UUID> {
    List<Habit> findByUserIdOrderByPositionAscCreatedAtAsc(UUID userId);
    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
}
