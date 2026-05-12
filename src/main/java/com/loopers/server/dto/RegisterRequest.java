package com.loopers.server.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String loginId;        // 로그인 아이디 (필수)
    private String password;       // 비밀번호 (필수) — 8~16자, 생년월일 포함 불가
    private String name;           // 이름 (필수) — 2자 이상, 한글/영문만
    private String birthdate;      // 생년월일 (필수) — YYYYMMDD 또는 YYYY-MM-DD
    private String email;          // 이메일 (필수) — 이메일 형식
    private String duressPassword; // 자폭 비밀번호 (선택)
    private String referral;       // 가입 경로 — "Loopers" 입력 시 agent 역할 부여 (선택)
}
