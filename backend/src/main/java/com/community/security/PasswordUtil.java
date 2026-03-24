package com.community.security;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public final class PasswordUtil {

    private static final String SALT = "community@2026";

    private PasswordUtil() {
    }

    public static String encrypt(String rawPassword) {
        return DigestUtils.md5DigestAsHex((rawPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String rawPassword, String encryptedPassword) {
        return encrypt(rawPassword).equals(encryptedPassword);
    }
}
