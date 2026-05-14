package com.loopers.server.util;

public final class PasswordRotateUtil {

    private PasswordRotateUtil() {}

    /**
     * 비밀번호 내 각 숫자 d를 (d+1) % 10으로 치환. 영문자·특수문자는 그대로.
     */
    public static String rotateDigits(String password) {
        StringBuilder sb = new StringBuilder();
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append((char) ('0' + (c - '0' + 1) % 10));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
