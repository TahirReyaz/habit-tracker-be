package com.tally.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A public landing response for the API's base URL, so opening it in a browser shows
 * that the service is up instead of a bare 401. Everything under /api (except /api/auth) needs a session.
 */
@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "service", "habit-tracker-api",
                "status", "ok",
                "health", "/actuator/health",
                "note", "This is the API. Open the frontend URL to use the app.");
    }
}
