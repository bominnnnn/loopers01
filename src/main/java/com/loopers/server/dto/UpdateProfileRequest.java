package com.loopers.server.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;  // 변경할 이름 (선택)
    private String email; // 변경할 이메일 (선택)
}
