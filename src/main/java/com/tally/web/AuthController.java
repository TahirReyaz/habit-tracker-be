package com.tally.web;

import com.tally.domain.User;
import com.tally.security.JwtService;
import com.tally.security.SessionCookies;
import com.tally.service.AuthService;
import com.tally.web.dto.Dtos.AuthRequest;
import com.tally.web.dto.Dtos.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final JwtService jwt;
    private final SessionCookies cookies;

    public AuthController(AuthService auth, JwtService jwt, SessionCookies cookies) {
        this.auth = auth;
        this.jwt = jwt;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody AuthRequest r) {
        return withSession(auth.register(r.email(), r.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody AuthRequest r, HttpServletRequest req) {
        return withSession(auth.login(r.email(), r.password(), Http.clientIp(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    private ResponseEntity<MeResponse> withSession(User u) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(jwt.issue(u.getId())).toString())
                .body(MeResponse.of(u));
    }
}
