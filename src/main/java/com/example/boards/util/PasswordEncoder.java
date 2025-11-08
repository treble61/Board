package com.example.boards.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoder {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public static String encode(String password) {
        return encoder.encode(password);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        // Support legacy SHA-256 hashes during migration
        if (encodedPassword.length() == 64 && !encodedPassword.startsWith("$2")) {
            // Legacy SHA-256 hash (64 hex characters)
            return org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawPassword).equals(encodedPassword);
        }
        // BCrypt hash
        return encoder.matches(rawPassword, encodedPassword);
    }
}
