package com.loopers.server.dto;

import com.loopers.server.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostResponse {
    private int id;                  // 게시글 id
    private String subject;          // 과목
    private String grade;            // 학년
    private String publisher;        // 출판사
    private String page;             // 페이지 번호
    private String type;             // 게시글 유형
    private String content;          // 본문 (감시 대상에게는 가짜 내용 반환)
    private String secretContent;    // 요원 전용 비밀 내용 (감시 대상에게는 가짜 반환)
    private int reporterId;          // 작성자 유저 id
    private String reporterLoginId;  // 작성자 아이디 (조회 시 join 대신 직접 채움)
    private String reporterName;     // 작성자 이름
    private int likes;               // 추천 수
    private String createdAt;        // 작성 날짜

    public static PostResponse from(Post post, String reporterLoginId, String reporterName) {
        return new PostResponse(
                post.getId(),
                post.getSubject(),
                post.getGrade(),
                post.getPublisher(),
                post.getPage(),
                post.getType(),
                post.getContent(),
                post.getSecretContent(),
                post.getReporterId(),
                reporterLoginId,
                reporterName,
                post.getLikes(),
                post.getCreatedAt()
        );
    }
}
