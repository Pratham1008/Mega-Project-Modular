package com.megaproject.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Shared password generator — extracted from ProfileService and BulkImportService
 * to eliminate duplication. Single source of truth for credential generation.
 */
@Component
public class PasswordGeneratorUtil {

    private static final String CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int DEFAULT_LENGTH = 12;
    private final SecureRandom rng = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
