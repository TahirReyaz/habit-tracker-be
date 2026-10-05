package com.tally.security;

import com.tally.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SessionCookies {
    public static final String NAME = "tally_session";
    private final AppProperties props;
    private final JwtService jwt;

    public SessionCookies(AppProperties props, JwtService jwt) {
        this.props = props;
        this.jwt = jwt;
    }

    public ResponseCookie create(String token) {
        return base(token).maxAge(jwt.ttl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(props.secureCookies())
                .sameSite("Strict")
                .path("/");
    }
}
