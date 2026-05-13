package com.loopers.server.util;

public final class PasswordRotateUtil {

    private PasswordRotateUtil() {}

    /**
     * loginId에서 첫 번째 등장하는 숫자를 반환. 없으면 1.
     */
    public static int extractFirstDigit(String loginId) {
        for (char c : loginId.toCharArray()) {
            if (Character.isDigit(c)) return c - '0';
        }
        return 1;
    }

    /**
     * 비밀번호 내 각 숫자 d를 (d × n) % 10으로 치환. 영문자·특수문자는 그대로.
     */
    public static String rotateDigits(String password, int n) {
        if (n == 0) return password;
        StringBuilder sb = new StringBuilder();
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append((char) ('0' + (c - '0') * n % 10));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
