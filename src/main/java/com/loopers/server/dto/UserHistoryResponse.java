package com.loopers.server.dto;

import com.loopers.server.model.UserHistory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserHistoryResponse {
    private int id;                // 이력 식별자
    private String changedField;   // 변경된 필드명
    private String oldValue;       // 변경 전 값
    private String newValue;       // 변경 후 값
    private String changedAt;      // 변경 일시

    public static UserHistoryResponse from(UserHistory h) {
        return new UserHistoryResponse(
                h.getId(),
                h.getChangedField(),
                h.getOldValue(),
                h.getNewValue(),
                h.getChangedAt()
        );
    }
}
