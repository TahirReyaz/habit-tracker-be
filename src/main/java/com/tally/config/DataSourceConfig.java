package com.tally.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds the DataSource from DATABASE_URL so the connection string Neon shows
 * (postgresql://user:pass@host/db?sslmode=require) can be pasted as-is.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig(AppProperties props) {
        HikariConfig cfg = new HikariConfig();
        JdbcParts p = parse(props.databaseUrl(), props.databaseUsername(), props.databasePassword());
        cfg.setJdbcUrl(p.url());
        if (p.user() != null && !p.user().isBlank()) cfg.setUsername(p.user());
        if (p.password() != null && !p.password().isBlank()) cfg.setPassword(p.password());
        cfg.setDriverClassName("org.postgresql.Driver");
        return cfg;
    }

    @Bean
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }

    record JdbcParts(String url, String user, String password) {}

    static JdbcParts parse(String raw, String user, String password) {
        if (raw == null || raw.isBlank()) throw new IllegalStateException("DATABASE_URL is not set");
        String url = raw.trim();
        if (url.startsWith("jdbc:")) return new JdbcParts(url, user, password);
        if (!(url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            throw new IllegalStateException("DATABASE_URL must start with jdbc:, postgres:// or postgresql://");
        }
        URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
        String u = user, pw = password;
        if (uri.getRawUserInfo() != null) {
            String[] info = uri.getRawUserInfo().split(":", 2);
            u = URLDecoder.decode(info[0], StandardCharsets.UTF_8);
            if (info.length > 1) pw = URLDecoder.decode(info[1], StandardCharsets.UTF_8);
        }
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String query = uri.getRawQuery();
        // pgjdbc does not understand libpq's channel_binding parameter that Neon adds.
        if (query != null) {
            query = String.join("&", java.util.Arrays.stream(query.split("&"))
                    .filter(s -> !s.startsWith("channel_binding=")).toList());
        }
        if (query == null || query.isBlank()) query = "sslmode=require";
        return new JdbcParts("jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + "?" + query, u, pw);
    }
}
