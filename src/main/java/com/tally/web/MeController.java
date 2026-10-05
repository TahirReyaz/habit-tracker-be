package com.tally.web;

import com.tally.security.CurrentUser;
import com.tally.security.SessionCookies;
import com.tally.service.AccountService;
import com.tally.service.LogService;
import com.tally.web.dto.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final AccountService accounts;
    private final LogService logs;
    private final SessionCookies cookies;

    public MeController(AccountService accounts, LogService logs, SessionCookies cookies) {
        this.accounts = accounts;
        this.logs = logs;
        this.cookies = cookies;
    }

    @GetMapping
    public MeResponse me() { return MeResponse.of(accounts.get(CurrentUser.id())); }

    @PatchMapping("/settings")
    public MeResponse settings(@Valid @RequestBody SettingsRequest r) {
        return MeResponse.of(accounts.updateSettings(CurrentUser.id(), r));
    }

    @PutMapping("/pin")
    public MeResponse pin(@Valid @RequestBody PinRequest r) {
        return MeResponse.of(accounts.setPin(CurrentUser.id(), r));
    }

    @PostMapping("/unlock")
    public Map<String, Boolean> unlock(@Valid @RequestBody UnlockRequest r) {
        if (!accounts.unlock(CurrentUser.id(), r.pin())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong PIN");
        return Map.of("ok", true);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> password(@Valid @RequestBody ChangePasswordRequest r) {
        accounts.changePassword(CurrentUser.id(), r);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tally-export.json\"")
                .body(accounts.export(CurrentUser.id()));
    }

    @PostMapping("/import")
    public ImportResult importData(@Valid @RequestBody ImportRequest r, @RequestParam(required = false) String today) {
        return logs.importData(CurrentUser.id(), r, Http.today(today));
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@Valid @RequestBody PasswordRequest r) {
        accounts.delete(CurrentUser.id(), r.password());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }
}
