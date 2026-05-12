package com.loopers.server.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String loginId;  // 로그인 아이디
    private String password; // 비밀번호 (자폭·과거 열쇠 비번 포함)
}
