package com.loopers.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.server.repository.InMemoryUserHistoryRepository;
import com.loopers.server.repository.InMemoryUserRepository;
import com.loopers.server.security.TokenBlacklist;
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
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InMemoryUserRepository userRepository;
    @Autowired InMemoryUserHistoryRepository historyRepository;
    @Autowired TokenBlacklist tokenBlacklist;

    private static final Map<String, String> TEST_USER = Map.of(
            "loginId", "testuser",
            "name", "테스트유저",
            "birthdate", "19990101",
            "email", "test@test.com",
            "password", "Test1234!"
    );

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.clear();
        historyRepository.clear();
        tokenBlacklist.clear();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TEST_USER)))
                .andExpect(status().isCreated());

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "testuser",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // ── 회원가입 ──────────────────────────────────────────────────

    @Test
    @DisplayName("정상 회원가입 → 201 + token")
    void register_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "newuser",
                                "name", "새유저",
                                "birthdate", "19900101",
                                "email", "new@test.com",
                                "password", "New1234!"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.loginId").value("newuser"));
    }

    // ── 로그인 ────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 로그인 → 200 + token")
    void login_success() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "testuser",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // ── 내 정보 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("정상 조회 → 200 + loginId")
    void getMe_success() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("testuser"));
    }

    // ── 내 정보 수정 ──────────────────────────────────────────────

    // ── 로그아웃 ──────────────────────────────────────────────────

    @Test
    @DisplayName("로그아웃 → 200")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("로그아웃 후 같은 토큰으로 요청 → 401")
    void logout_tokenInvalidated() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ── 비밀번호 변경 ─────────────────────────────────────────────

    // ── 과거 열쇠 함정 ────────────────────────────────────────────

    // ── 자폭 비밀번호 ─────────────────────────────────────────────

    // ── 요원 비밀번호 자동 변경 ───────────────────────────────────

    @Test
    @DisplayName("요원 로그인 → 기존 비밀번호로 재로그인 시 함정")
    void agentLogin_passwordAutoChanged() throws Exception {
        // "Agent1234!" → 각 숫자 +1 → "Agent2345!"
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent1",
                        "name", "에이전트",
                        "birthdate", "19900101",
                        "email", "agent1@test.com",
                        "password", "Agent1234!",
                        "referral", "Loopers"
                ))));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent1",
                        "password", "Agent1234!"
                ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "agent1",
                                "password", "Agent1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trap").value("old_key"));
    }

    @Test
    @DisplayName("요원 로그인 후 새 비밀번호(각 숫자 +1)로 재로그인 성공")
    void agentLogin_newPasswordWorks() throws Exception {
        // "Agent1234!" → "Agent2345!"
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent1",
                        "name", "에이전트",
                        "birthdate", "19900101",
                        "email", "agent1@test.com",
                        "password", "Agent1234!",
                        "referral", "Loopers"
                ))));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent1", "password", "Agent1234!"
                ))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "agent1",
                                "password", "Agent2345!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // ── 회원 탈퇴 ────────────────────────────────────────────────

    // ── 비밀번호 유효성 검사 ──────────────────────────────────────

    // ── 수정 이력 ─────────────────────────────────────────────────
}
