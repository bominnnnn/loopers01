package com.loopers.server.model;

import lombok.Data;

@Data
public class UserHistory {
    private int id;                // 이력 식별자
    private int userId;            // 수정된 유저 id
    private String changedField;   // 변경된 필드명 (name / email / password)
    private String oldValue;       // 변경 전 값 (비밀번호는 "******" 마스킹)
    private String newValue;       // 변경 후 값 (비밀번호는 "******" 마스킹)
    private String changedAt;      // 변경 일시
}
