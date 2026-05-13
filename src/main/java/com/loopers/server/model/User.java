package com.loopers.server.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class User {
    private int id;                              // 내부 식별자
    private String loginId;                      // 로그인 아이디
    private String name;                         // 이름
    private String birthdate;                    // 생년월일 (YYYYMMDD)
    private String email;                        // 이메일
    private String passwordHash;                 // 비밀번호 해시 (bcrypt)
    private String duressPasswordHash;           // 자폭 비밀번호 해시 — 입력 시 계정 전체 삭제
    private List<String> passwordHistory = new ArrayList<>(); // 이전 비밀번호 해시 목록 — 과거 열쇠 함정용
    private String role;                         // civilian(일반) | agent(요원) | watchlisted_agent(감시 대상 요원)
    private String createdAt;                    // 가입 일시
}
