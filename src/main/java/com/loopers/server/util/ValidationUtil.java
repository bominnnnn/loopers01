package com.loopers.server.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ValidationUtil {

    private static final DateTimeFormatter FMT_PLAIN = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT_DASH  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 이름: 2자 이상, 한글 또는 영문만 */
    public static List<String> validateName(String name) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("이름을 입력해주세요.");
            return errors;
        }
        if (name.length() < 2) {
            errors.add("이름은 2자 이상이어야 합니다.");
        }
        if (!name.matches("[가-힣a-zA-Z]+")) {
            errors.add("이름은 한글 또는 영문만 입력 가능합니다.");
        }
        return errors;
    }

    /** 이메일: 기본 이메일 형식 */
    public static List<String> validateEmail(String email) {
        List<String> errors = new ArrayList<>();
        if (email == null || email.isBlank()) {
            errors.add("이메일을 입력해주세요.");
            return errors;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            errors.add("이메일 형식이 올바르지 않습니다.");
        }
        return errors;
    }

    /**
     * 생년월일: YYYYMMDD 또는 YYYY-MM-DD 형식, 실제 존재하는 날짜
     * 정상이면 YYYYMMDD 형태의 문자열 반환, 오류면 null 반환
     */
    public static String normalizeBirthdate(String birthdate, List<String> errors) {
        if (birthdate == null || birthdate.isBlank()) {
            errors.add("생년월일을 입력해주세요.");
            return null;
        }
        String cleaned = birthdate.replace("-", "");
        if (!cleaned.matches("[0-9]{8}")) {
            errors.add("생년월일은 YYYYMMDD 또는 YYYY-MM-DD 형식이어야 합니다.");
            return null;
        }
        try {
            LocalDate.parse(cleaned, FMT_PLAIN); // 실제 날짜 유효성 검사
        } catch (DateTimeParseException e) {
            errors.add("존재하지 않는 날짜입니다.");
            return null;
        }
        return cleaned;
    }

    /**
     * 비밀번호 규칙:
     * 1. 8~16자의 영문 대소문자, 숫자, 특수문자만 가능
     * 2. 생년월일(YYYYMMDD)이 비밀번호 내에 포함될 수 없음
     */
    public static List<String> validatePassword(String password, String birthdateYYYYMMDD) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isBlank()) {
            errors.add("비밀번호를 입력해주세요.");
            return errors;
        }
        if (password.length() < 8 || password.length() > 16) {
            errors.add("비밀번호는 8~16자여야 합니다.");
        }
        if (!password.matches("[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?]+")) {
            errors.add("비밀번호는 영문 대소문자, 숫자, 특수문자만 사용 가능합니다.");
        }
        if (birthdateYYYYMMDD != null) {
            String year  = birthdateYYYYMMDD.substring(0, 4); // YYYY
            String mmdd  = birthdateYYYYMMDD.substring(4);     // MMDD
            String yymmdd = birthdateYYYYMMDD.substring(2);    // YYMMDD
            if (password.contains(birthdateYYYYMMDD) || password.contains(yymmdd)
                    || password.contains(mmdd) || password.contains(year)) {
                errors.add("비밀번호에 생년월일이 포함될 수 없습니다.");
            }
        }
        return errors;
    }
}
