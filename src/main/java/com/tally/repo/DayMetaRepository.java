package com.tally.repo;

import com.tally.domain.DayMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DayMetaRepository extends JpaRepository<DayMeta, UUID> {
    List<DayMeta> findByUserIdAndDayBetween(UUID userId, LocalDate from, LocalDate to);
    List<DayMeta> findByUserIdOrderByDayAsc(UUID userId);
    Optional<DayMeta> findByUserIdAndDay(UUID userId, LocalDate day);
}
