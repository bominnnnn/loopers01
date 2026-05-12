package com.loopers.server.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword; // 현재 비밀번호 (본인 확인용)
    private String newPassword;     // 새 비밀번호 — 변경 후 이전 비번은 과거 열쇠 함정에 등록됨
}
