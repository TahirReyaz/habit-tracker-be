package com.tally.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the DataSource from DATABASE_URL. Every form Neon shows works as-is:
 * <ul>
 *   <li>{@code postgresql://user:pass@ep-x.neon.tech/neondb?sslmode=require} (connection string)</li>
 *   <li>{@code jdbc:postgresql://ep-x.neon.tech/neondb?user=...&password=...&sslmode=require} (JDBC tab)</li>
 *   <li>{@code psql 'postgresql://...'} (the psql snippet, quotes and all)</li>
 * </ul>
 * DATABASE_USERNAME / DATABASE_PASSWORD are only needed when the URL carries no credentials,
 * and override what the URL says when set.
 */
@Configuration
public class DataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig(AppProperties props) {
        JdbcParts p = parse(props.databaseUrl(), props.databaseUsername(), props.databasePassword());
        if (p.user() == null || p.user().isBlank()) {
            throw new IllegalStateException("No database user: put it in DATABASE_URL (postgresql://user:password@host/db) "
                    + "or set DATABASE_USERNAME and DATABASE_PASSWORD.");
        }
        log.info("Database: {} as user '{}'", p.url().replaceAll("[?].*$", ""), p.user());
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(p.url());
        cfg.setUsername(p.user());
        if (p.password() != null) cfg.setPassword(p.password());
        cfg.setDriverClassName("org.postgresql.Driver");
        return cfg;
    }

    @Bean
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }

    /** url never contains credentials; user/password come from the URL unless explicitly overridden. */
    record JdbcParts(String url, String user, String password) {}

    static JdbcParts parse(String raw, String userOverride, String passwordOverride) {
        if (raw == null || raw.isBlank()) throw new IllegalStateException("DATABASE_URL is not set");
        String url = raw.trim();
        if (url.startsWith("psql ")) url = url.substring(5).trim();       // pasted the psql snippet
        url = url.replaceAll("^['\"]|['\"]$", "");                         // surrounding quotes
        if (url.startsWith("jdbc:")) url = url.substring(5);
        if (!(url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            throw new IllegalStateException("DATABASE_URL must look like postgresql://user:password@host/db "
                    + "or jdbc:postgresql://host/db (got something starting with '" + url.substring(0, Math.min(12, url.length())) + "')");
        }
        URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
        if (uri.getHost() == null) throw new IllegalStateException("DATABASE_URL has no host");

        String user = null, password = null;
        if (uri.getRawUserInfo() != null) {
            String[] info = uri.getRawUserInfo().split(":", 2);
            user = decode(info[0]);
            if (info.length > 1) password = decode(info[1]);
        }
        // query: lift user/password out (JDBC style), drop libpq-only params, keep the rest
        List<String> keep = new ArrayList<>();
        boolean hasSsl = false;
        if (uri.getRawQuery() != null) {
            for (String kv : uri.getRawQuery().split("&")) {
                if (kv.isBlank()) continue;
                String k = kv.contains("=") ? kv.substring(0, kv.indexOf('=')) : kv;
                String v = kv.contains("=") ? decode(kv.substring(kv.indexOf('=') + 1)) : "";
                switch (k) {
                    case "user" -> user = v;
                    case "password" -> password = v;
                    case "channel_binding" -> { } // libpq only; pgjdbc doesn't understand it
                    default -> {
                        if (k.equals("sslmode")) hasSsl = true;
                        keep.add(kv);
                    }
                }
            }
        }
        boolean local = uri.getHost().equals("localhost") || uri.getHost().startsWith("127.") || uri.getHost().equals("host.docker.internal");
        if (!hasSsl && !local) keep.add("sslmode=require");

        if (userOverride != null && !userOverride.isBlank()) user = userOverride.trim();
        if (passwordOverride != null && !passwordOverride.isBlank()) password = passwordOverride;

        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/postgres" : uri.getRawPath();
        String jdbc = "jdbc:postgresql://" + uri.getHost() + port + path + (keep.isEmpty() ? "" : "?" + String.join("&", keep));
        return new JdbcParts(jdbc, user, password);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
