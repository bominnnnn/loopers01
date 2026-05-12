package com.loopers.server.model;

import lombok.Data;

@Data
public class Post {
    private int id;              // 내부 식별자
    private String subject;      // 과목 (예: 수학, 영어)
    private String grade;        // 학년
    private String publisher;    // 출판사
    private String page;         // 페이지 번호
    private String type;         // 게시글 유형
    private String content;      // 본문 내용
    private String secretContent;// 요원 전용 비밀 내용
    private int reporterId;      // 작성자 유저 id
    private int likes;           // 추천 수
    private String createdAt;    // 작성 날짜
}
