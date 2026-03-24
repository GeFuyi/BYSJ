package com.community.enums;

import java.util.Arrays;

public enum UserRole {
    ADMIN,
    EMPLOYEE,
    USER;

    public static boolean isValid(String role) {
        return Arrays.stream(values()).anyMatch(item -> item.name().equals(role));
    }
}
