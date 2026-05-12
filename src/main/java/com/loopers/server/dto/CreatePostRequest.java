package com.loopers.server.dto;

import lombok.Data;

@Data
public class CreatePostRequest {
    private String subject;       // 과목 (필수)
    private String grade;         // 학년 (선택)
    private String publisher;     // 출판사 (선택)
    private String page;          // 페이지 번호 (필수)
    private String type;          // 게시글 유형 (선택)
    private String content;       // 본문 내용 (필수)
    private String secretContent; // 요원 전용 비밀 내용 (선택 — 프론트에서 agent만 입력 가능하도록 제어)
}
