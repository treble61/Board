package com.example.boards.util;

import org.apache.commons.codec.digest.DigestUtils;

public class PasswordEncoder {

    public static String encode(String password) {
        return DigestUtils.sha256Hex(password);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
