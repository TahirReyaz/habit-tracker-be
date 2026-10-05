package com.tally.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof UUID id)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }
}
