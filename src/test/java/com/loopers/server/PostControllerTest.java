package com.loopers.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.server.repository.InMemoryPostRepository;
import com.loopers.server.repository.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InMemoryUserRepository userRepository;
    @Autowired InMemoryPostRepository postRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.clear();
        postRepository.clear();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "poster",
                        "name", "작성자",
                        "birthdate", "19900101",
                        "email", "poster@test.com",
                        "password", "Post1234!"
                ))));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "poster",
                                "password", "Post1234!"
                        ))))
                .andReturn();

        token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // ── 게시글 작성 ──────────────────────────────────────────────

    @Test
    @DisplayName("게시글 작성 → 201")
    void createPost_success() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "수학",
                                "content", "내용입니다",
                                "page", "42"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.post.subject").value("수학"))
                .andExpect(jsonPath("$.post.reporterLoginId").value("poster"));
    }

    @Test
    @DisplayName("필수 항목 누락 → 400")
    void createPost_missingFields() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("subject", "수학"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 없이 작성 → 401")
    void createPost_noToken() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "수학", "content", "내용", "page", "1"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── 게시글 목록 ──────────────────────────────────────────────

    @Test
    @DisplayName("게시글 목록 조회 → 200")
    void getPosts_success() throws Exception {
        // 게시글 2개 작성
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/api/posts")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "subject", "수학",
                            "content", "내용 " + i,
                            "page", String.valueOf(i)
                    ))));
        }

        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2));
    }

    @Test
    @DisplayName("subject 필터링 → 해당 subject만 반환")
    void getPosts_filterBySubject() throws Exception {
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "subject", "수학", "content", "수학 내용", "page", "1"
                ))));
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "subject", "영어", "content", "영어 내용", "page", "2"
                ))));

        mockMvc.perform(get("/api/posts?subject=수학")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].subject").value("수학"));
    }

    // ── 게시글 단건 ──────────────────────────────────────────────

    @Test
    @DisplayName("게시글 단건 조회 → 200")
    void getPost_success() throws Exception {
        var result = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "국어", "content", "내용", "page", "10"
                        ))))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("post").get("id").asInt();

        mockMvc.perform(get("/api/posts/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.post.id").value(id));
    }

    @Test
    @DisplayName("없는 게시글 조회 → 404")
    void getPost_notFound() throws Exception {
        mockMvc.perform(get("/api/posts/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── 게시글 추천 ──────────────────────────────────────────────

    @Test
    @DisplayName("게시글 추천 → likes 증가")
    void likePost_success() throws Exception {
        var result = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "subject", "과학", "content", "내용", "page", "5"
                        ))))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("post").get("id").asInt();

        mockMvc.perform(post("/api/posts/" + id + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1));
    }

    // ── 랭킹 ────────────────────────────────────────────────────

    @Test
    @DisplayName("랭킹 조회 → 200")
    void getRanking_success() throws Exception {
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "subject", "수학", "content", "내용", "page", "1"
                ))));

        mockMvc.perform(get("/api/posts/ranking/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byLikes").exists())
                .andExpect(jsonPath("$.reporters").exists());
    }
}
