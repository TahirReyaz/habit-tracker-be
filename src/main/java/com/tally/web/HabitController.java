package com.tally.web;

import com.tally.security.CurrentUser;
import com.tally.service.HabitService;
import com.tally.web.dto.Dtos.HabitDto;
import com.tally.web.dto.Dtos.HabitRequest;
import com.tally.web.dto.Dtos.ReorderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/habits")
public class HabitController {
    private final HabitService habits;

    public HabitController(HabitService habits) { this.habits = habits; }

    @GetMapping
    public List<HabitDto> list() {
        return habits.list(CurrentUser.id()).stream().map(HabitDto::of).toList();
    }

    @PostMapping
    public HabitDto create(@Valid @RequestBody HabitRequest r, @RequestParam(required = false) String today) {
        return HabitDto.of(habits.create(CurrentUser.id(), r, Http.today(today)));
    }

    @PatchMapping("/{id}")
    public HabitDto update(@PathVariable UUID id, @Valid @RequestBody HabitRequest r) {
        return HabitDto.of(habits.update(CurrentUser.id(), id, r));
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorder(@Valid @RequestBody ReorderRequest r) {
        habits.reorder(CurrentUser.id(), r.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        habits.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
