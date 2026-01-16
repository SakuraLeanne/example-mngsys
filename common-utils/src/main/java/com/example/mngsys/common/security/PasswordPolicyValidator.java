package com.example.mngsys.common.security;

import org.springframework.util.StringUtils;

/**
 * PasswordPolicyValidator。
 * <p>
 * 统一密码长度与复杂度校验规则，要求至少 8 位且包含字母与数字。
 * </p>
 */
public final class PasswordPolicyValidator {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicyValidator() {
    }

    public static boolean isValid(String password) {
        return StringUtils.hasText(password)
                && password.length() >= MIN_LENGTH
                && isComplexEnough(password);
    }

    private static boolean isComplexEnough(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }
}
