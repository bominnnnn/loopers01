package com.loopers.server.repository;

import com.loopers.server.model.UserHistory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
public class InMemoryUserHistoryRepository {

    private final List<UserHistory> histories = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    /**
     * 수정 이력 저장
     * @param userId       수정된 유저 id
     * @param changedField 변경된 필드명
     * @param oldValue     변경 전 값
     * @param newValue     변경 후 값
     */
    public UserHistory record(int userId, String changedField, String oldValue, String newValue) {
        UserHistory history = new UserHistory();
        history.setId(idCounter.getAndIncrement());
        history.setUserId(userId);
        history.setChangedField(changedField);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedAt(Instant.now().toString());
        histories.add(history);
        return history;
    }

    /** 특정 유저의 전체 수정 이력 조회 (최신순) */
    public List<UserHistory> findByUserId(int userId) {
        List<UserHistory> result = histories.stream()
                .filter(h -> h.getUserId() == userId)
                .collect(Collectors.toList());
        // 최신순 정렬
        java.util.Collections.reverse(result);
        return result;
    }

    /** 테스트 격리용 — 전체 데이터 초기화 */
    public void clear() {
        histories.clear();
        idCounter.set(1);
    }
}
