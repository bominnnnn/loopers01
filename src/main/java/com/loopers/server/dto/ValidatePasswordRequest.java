package com.loopers.server.dto;

import lombok.Data;

@Data
public class ValidatePasswordRequest {
    private String password; // 유효성을 검사할 비밀번호 (8자 이상, 대문자·숫자·특수문자 포함)
}
