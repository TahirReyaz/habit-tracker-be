package com.tally.crypto;

import com.tally.config.AppProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * JPA converter that transparently encrypts a String column.
 * Usage: {@code @Convert(converter = Encrypted.class)}.
 */
@Component
@Converter
public class Encrypted implements AttributeConverter<String, String> {
    private static final Logger log = LoggerFactory.getLogger(Encrypted.class);
    private static volatile FieldCipher cipher;

    public Encrypted() {}

    @org.springframework.beans.factory.annotation.Autowired
    public Encrypted(AppProperties props) {
        init(props.encryptionKey());
    }

    private static synchronized void init(String b64) {
        if (cipher != null) return;
        byte[] key;
        if (b64 == null || b64.isBlank()) {
            log.warn("APP_ENCRYPTION_KEY not set - using an insecure development key. Never do this in production.");
            key = sha256("tally-dev-only-key");
        } else {
            key = Base64.getDecoder().decode(b64.trim());
        }
        cipher = new FieldCipher(key);
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static FieldCipher cipher() {
        if (cipher == null) throw new IllegalStateException("Encryption not initialised");
        return cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : cipher().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : cipher().decrypt(dbData);
    }
}
