package com.loopers.server.dto;

import com.loopers.server.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private String loginId;   // 로그인 아이디
    private String name;      // 이름
    private String birthdate; // 생년월일 (YYYYMMDD)
    private String email;     // 이메일
    private String role;      // 역할 (civilian / agent / watchlisted_agent)

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getLoginId(),
                user.getName(),
                user.getBirthdate(),
                user.getEmail(),
                user.getRole()
        );
    }
}
