package com.tally.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String databaseUrl,
        String databaseUsername,
        String databasePassword,
        String encryptionKey,
        String jwtSecret,
        int sessionDays,
        boolean secureCookies,
        String corsOrigins,
        boolean allowRegistration
) {}
