package com.tally.service;

import com.tally.config.AppProperties;
import com.tally.domain.User;
import com.tally.repo.UserRepository;
import com.tally.security.AttemptLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AttemptLimiter limiter;
    private final AppProperties props;

    public AuthService(UserRepository users, PasswordEncoder encoder, AttemptLimiter limiter, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.limiter = limiter;
        this.props = props;
    }

    @Transactional
    public User register(String email, String password) {
        if (!props.allowRegistration()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is closed");
        String e = normalize(email);
        if (users.existsByEmailIgnoreCase(e)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        return users.save(new User(e, encoder.encode(password)));
    }

    public User login(String email, String password, String ip) {
        String e = normalize(email);
        String key = "login:" + ip + ":" + e;
        limiter.check(key);
        User u = users.findByEmailIgnoreCase(e).orElse(null);
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            limiter.fail(key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        limiter.reset(key);
        return u;
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
