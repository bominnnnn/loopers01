package com.loopers.server.vo;

import com.loopers.server.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public final class Password {

    private final String value;

    /** 생년월일 포함 여부까지 검증 (회원가입용) */
    public Password(String raw, Birthdate birthdate) {
        List<String> errors = validate(raw, birthdate != null ? birthdate.getValue() : null);
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, String.join(" ", errors));
        }
        this.value = raw;
    }

    /** 형식만 검증 (비밀번호 유효성 검사 엔드포인트용) */
    public Password(String raw) {
        this(raw, null);
    }

    public String getValue() { return value; }

    /**
     * 비밀번호 규칙 검사 — 에러 목록 반환
     * 규칙:
     *   1. 8~16자의 영문 대소문자, 숫자, 특수문자만 가능
     *   2. 생년월일(YYYYMMDD)이 포함될 수 없음
     */
    public static List<String> validate(String raw, String birthdateYYYYMMDD) {
        List<String> errors = new ArrayList<>();

        if (raw == null || raw.isBlank()) {
            errors.add("비밀번호를 입력해주세요.");
            return errors;
        }
        if (raw.length() < 8 || raw.length() > 16) {
            errors.add("비밀번호는 8~16자여야 합니다.");
        }
        if (!raw.matches("[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?]+")) {
            errors.add("비밀번호는 영문 대소문자, 숫자, 특수문자만 사용 가능합니다.");
        }
        if (birthdateYYYYMMDD != null && containsBirthdate(raw, birthdateYYYYMMDD)) {
            errors.add("비밀번호에 생년월일이 포함될 수 없습니다.");
        }

        return errors;
    }

    private static boolean containsBirthdate(String password, String bd) {
        String yyyymmdd = bd;                    // 19900415
        String yymmdd   = bd.substring(2);       // 900415
        String mmdd     = bd.substring(4);       // 0415
        String yyyy     = bd.substring(0, 4);    // 1990
        return password.contains(yyyymmdd)
            || password.contains(yymmdd)
            || password.contains(mmdd)
            || password.contains(yyyy);
    }
}
