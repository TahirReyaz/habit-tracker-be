package com.tally.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataSourceConfigTest {
    @Test
    void parsesNeonUrl() {
        var p = DataSourceConfig.parse(
                "postgresql://neondb_owner:p%40ss@ep-cool-dew-123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require",
                null, null);
        assertEquals("jdbc:postgresql://ep-cool-dew-123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require", p.url());
        assertEquals("neondb_owner", p.user());
        assertEquals("p@ss", p.password());
    }

    @Test
    void passesJdbcThrough() {
        var p = DataSourceConfig.parse("jdbc:postgresql://localhost:5432/tally", "u", "pw");
        assertEquals("jdbc:postgresql://localhost:5432/tally", p.url());
        assertEquals("u", p.user());
    }
}
