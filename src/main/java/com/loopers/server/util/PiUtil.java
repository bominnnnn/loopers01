package com.loopers.server.util;

/**
 * π 소수점 자리 유틸
 * 요원 비밀번호 자동 변경 규칙에 사용
 * 예) agent42 → 42번째 소수점 자리 → 해당 숫자를 기존 비번 뒤에 붙임
 */
public class PiUtil {

    // π 소수점 이하 100자리
    private static final String PI_DECIMALS =
            "1415926535" +  //  1 ~ 10
            "8979323846" +  // 11 ~ 20
            "2643383279" +  // 21 ~ 30
            "5028841971" +  // 31 ~ 40
            "6939937510" +  // 41 ~ 50
            "5820974944" +  // 51 ~ 60
            "5923078164" +  // 61 ~ 70
            "0628620899" +  // 71 ~ 80
            "8628034825" +  // 81 ~ 90
            "3421170679";   // 91 ~ 100

    /**
     * n번째 소수점 자리 반환 (1-based)
     * 예) n=1 → '1',  n=5 → '9',  n=42 → '9'
     */
    public static char getDecimalDigit(int n) {
        if (n < 1 || n > PI_DECIMALS.length()) {
            throw new IllegalArgumentException(n + "번째 π 소수점 자리는 지원 범위(1~100)를 벗어났습니다.");
        }
        return PI_DECIMALS.charAt(n - 1);
    }

    /**
     * loginId에서 숫자만 추출
     * 예) "agent42" → 42,  "007bond" → 7,  "admin" → 1(기본값)
     */
    public static int extractNumber(String loginId) {
        String digits = loginId.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 1;
        return Integer.parseInt(digits);
    }
}
