package com.tally.repo;

import com.tally.domain.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {
    List<Entry> findByUserIdAndDayBetween(UUID userId, LocalDate from, LocalDate to);
    List<Entry> findByUserIdOrderByDayAsc(UUID userId);
    Optional<Entry> findByUserIdAndHabitIdAndDay(UUID userId, UUID habitId, LocalDate day);
    Optional<Entry> findFirstByUserIdOrderByDayAsc(UUID userId);

    /** (habitId, day) pairs only - avoids decrypting every note just to compute statistics. */
    @Query("select e.habitId, e.day from Entry e where e.userId = ?1")
    List<Object[]> findKeys(UUID userId);
}
