package com.tally.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads the session JWT from the httpOnly cookie (or a Bearer header for API clients). */
/** Not a @Component on purpose: it must only run inside the security chain. */
public class JwtCookieFilter extends OncePerRequestFilter {
    private final JwtService jwt;

    public JwtCookieFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = null;
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) if (SessionCookies.NAME.equals(c.getName())) token = c.getValue();
        }
        String auth = req.getHeader("Authorization");
        if (token == null && auth != null && auth.startsWith("Bearer ")) token = auth.substring(7);
        if (token != null && !token.isBlank()) {
            jwt.verify(token).ifPresent(uid -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(uid, null, List.of())));
        }
        chain.doFilter(req, res);
    }
}
