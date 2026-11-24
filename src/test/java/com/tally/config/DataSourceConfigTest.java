package com.tally.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {
    @Test
    void neonConnectionString() {
        var p = DataSourceConfig.parse(
                "postgresql://neondb_owner:p%40ss@ep-cool-dew-123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require",
                null, null);
        assertEquals("jdbc:postgresql://ep-cool-dew-123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require", p.url());
        assertEquals("neondb_owner", p.user());
        assertEquals("p@ss", p.password());
    }

    @Test
    void neonJdbcWithCredentialsInQuery() {
        var p = DataSourceConfig.parse(
                "jdbc:postgresql://ep-x.neon.tech/neondb?user=neondb_owner&password=secret&sslmode=require", "", "");
        assertEquals("jdbc:postgresql://ep-x.neon.tech/neondb?sslmode=require", p.url());
        assertEquals("neondb_owner", p.user());
        assertEquals("secret", p.password());
    }

    @Test
    void psqlSnippetAndMissingSsl() {
        var p = DataSourceConfig.parse("psql 'postgresql://u:pw@ep-x.neon.tech/neondb'", null, null);
        assertEquals("jdbc:postgresql://ep-x.neon.tech/neondb?sslmode=require", p.url());
        assertEquals("u", p.user());
    }

    @Test
    void separateCredentialsOverride() {
        var p = DataSourceConfig.parse("jdbc:postgresql://localhost:5432/tally", "tally", "tally");
        assertEquals("jdbc:postgresql://localhost:5432/tally", p.url());
        assertEquals("tally", p.user());
        assertEquals("tally", p.password());
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalStateException.class, () -> DataSourceConfig.parse("mysql://x", null, null));
    }
}
