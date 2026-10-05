package com.tally.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NotFound extends ResponseStatusException {
    public NotFound(String what) { super(HttpStatus.NOT_FOUND, what + " not found"); }
}
