package com.loopers.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.server.repository.InMemoryUserHistoryRepository;
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
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InMemoryUserRepository userRepository;
    @Autowired InMemoryUserHistoryRepository historyRepository;

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

        // 매 테스트 전 유저 등록 + 토큰 확보
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
    @DisplayName("정상 회원가입 → 201")
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

    @Test
    @DisplayName("중복 아이디 → 409")
    void register_duplicateLoginId() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TEST_USER)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("필수 항목 누락 → 400")
    void register_missingFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("loginId", "only"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 이메일 형식 → 400")
    void register_invalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "user2",
                                "name", "홍길동",
                                "birthdate", "19900101",
                                "email", "not-an-email",
                                "password", "Valid1234!"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 생년월일 형식 → 400")
    void register_invalidBirthdate() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "user3",
                                "name", "홍길동",
                                "birthdate", "990101",
                                "email", "user3@test.com",
                                "password", "Valid1234!"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호에 생년월일 포함 → 400")
    void register_passwordContainsBirthdate() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "user4",
                                "name", "홍길동",
                                "birthdate", "19900415",
                                "email", "user4@test.com",
                                "password", "Pw19900415!"   // 생년월일 포함
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 17자 초과 → 400")
    void register_passwordTooLong() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "user5",
                                "name", "홍길동",
                                "birthdate", "19900101",
                                "email", "user5@test.com",
                                "password", "TooLongPassword1!"  // 17자
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("헤더 인증으로 내 정보 조회 → 200")
    void getMe_headerAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("X-Loopers-LoginId", "testuser")
                        .header("X-Loopers-LoginPw", "Test1234!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("testuser"));
    }

    @Test
    @DisplayName("Loopers referral → role=agent")
    void register_agentRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "agent1",
                                "name", "에이전트",
                                "birthdate", "19900101",
                                "email", "agent@test.com",
                                "password", "Agent1234!",
                                "referral", "Loopers"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("agent"));
    }

    // ── 요원 비밀번호 자동 변경 ───────────────────────────────────────

    @Test
    @DisplayName("요원 로그인 성공 → 비밀번호 자동 변경 (각 숫자 d → d×n % 10)")
    void agentLogin_passwordAutoChanged() throws Exception {
        // agent2: n=2, "Agent1234!" → 1×2=2, 2×2=4, 3×2=6, 4×2=8 → "Agent2468!"
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent2",
                        "name", "에이전트",
                        "birthdate", "19900101",
                        "email", "agent2@test.com",
                        "password", "Agent1234!",
                        "referral", "Loopers"
                ))));

        // 로그인 → 내부적으로 비밀번호 자동 변경
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "agent2",
                                "password", "Agent1234!"
                        ))))
                .andExpect(status().isOk());

        // 기존 비밀번호로 재로그인 → 과거 열쇠 함정 (변경됐음을 간접 증명)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "agent2",
                                "password", "Agent1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trap").value("old_key"));
    }

    @Test
    @DisplayName("요원 로그인 후 새 비밀번호(각 숫자 d×n%10)로 재로그인 성공")
    void agentLogin_newPasswordWorks() throws Exception {
        // agent2: n=2, "Agent1234!" → "Agent2468!"
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent2",
                        "name", "에이전트",
                        "birthdate", "19900101",
                        "email", "agent2@test.com",
                        "password", "Agent1234!",
                        "referral", "Loopers"
                ))));

        // 첫 로그인 → 비밀번호 자동 변경
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "agent2", "password", "Agent1234!"
                ))));

        // 변경된 새 비밀번호로 로그인 → 성공
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "agent2",
                                "password", "Agent2468!"  // 1×2=2, 2×2=4, 3×2=6, 4×2=8
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("civilian 로그인 → 비밀번호 변경 없음")
    void civilianLogin_passwordNotChanged() throws Exception {
        // 일반 유저로 로그인
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "testuser",
                        "password", "Test1234!"
                ))));

        // 동일 비밀번호로 재로그인 → 성공 (변경 안 됐음)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "testuser",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trap").doesNotExist());
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

    @Test
    @DisplayName("잘못된 비밀번호 → 401")
    void login_wrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "testuser",
                                "password", "wrongpassword"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 아이디 → 404")
    void login_unknownUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "nobody",
                                "password", "1234"
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── 내 정보 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("토큰 없이 요청 → 401")
    void getMe_noToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("정상 조회 → 200")
    void getMe_success() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("testuser"));
    }

    // ── 내 정보 수정 ──────────────────────────────────────────────

    @Test
    @DisplayName("이름 수정 → 200")
    void updateProfile_success() throws Exception {
        mockMvc.perform(patch("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "새이름"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("새이름"));
    }

    // ── 비밀번호 변경 ─────────────────────────────────────────────

    @Test
    @DisplayName("비밀번호 변경 → 200")
    void changePassword_success() throws Exception {
        mockMvc.perform(patch("/api/auth/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "Test1234!",
                                "newPassword", "New5678@"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("현재 비밀번호 틀림 → 401")
    void changePassword_wrongCurrent() throws Exception {
        mockMvc.perform(patch("/api/auth/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongPass1!",
                                "newPassword", "New5678@"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── 과거 열쇠 함정 ────────────────────────────────────────────

    @Test
    @DisplayName("이전 비밀번호로 로그인 → 가짜 토큰 반환")
    void login_oldPasswordTrap() throws Exception {
        // 비밀번호 변경
        mockMvc.perform(patch("/api/auth/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "currentPassword", "Test1234!",
                        "newPassword", "New5678@"
                ))));

        // 이전 비밀번호로 로그인 → 함정
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "testuser",
                                "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trap").value("old_key"))
                .andExpect(jsonPath("$.user.isFake").value(true));
    }

    // ── 자폭 비밀번호 ─────────────────────────────────────────────

    @Test
    @DisplayName("자폭 비밀번호로 로그인 → 계정 삭제")
    void login_duressPassword() throws Exception {
        // duressPassword 포함 회원가입
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", "secretagent",
                        "name", "비밀요원",
                        "birthdate", "19900101",
                        "email", "secret@test.com",
                        "password", "Normal1!",
                        "duressPassword", "Destruct2@"
                ))));

        // 자폭 비밀번호로 로그인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "secretagent",
                                "password", "Destruct2@"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selfDestruct").value(true));

        // 계정이 삭제되었는지 확인
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", "secretagent",
                                "password", "Normal1!"
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── 회원 탈퇴 ────────────────────────────────────────────────

    @Test
    @DisplayName("회원 탈퇴 → 200")
    void deleteAccount_success() throws Exception {
        mockMvc.perform(delete("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // ── 비밀번호 유효성 검사 ──────────────────────────────────────

    @Test
    @DisplayName("유효한 비밀번호 → 200 valid:true")
    void validatePassword_valid() throws Exception {
        mockMvc.perform(post("/api/auth/validate-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "Secure1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("짧은 비밀번호 → 422 valid:false")
    void validatePassword_tooShort() throws Exception {
        mockMvc.perform(post("/api/auth/validate-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "short"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ── 수정 이력 ─────────────────────────────────────────────────

    @Test
    @DisplayName("이력 없을 때 조회 → 빈 배열")
    void getHistory_empty() throws Exception {
        mockMvc.perform(get("/api/auth/me/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history.length()").value(0));
    }

    @Test
    @DisplayName("이름 수정 → 이력 1건 적재")
    void getHistory_afterNameChange() throws Exception {
        mockMvc.perform(patch("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "새이름"))));

        mockMvc.perform(get("/api/auth/me/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].changedField").value("name"))
                .andExpect(jsonPath("$.history[0].oldValue").value("테스트유저"))
                .andExpect(jsonPath("$.history[0].newValue").value("새이름"));
    }

    @Test
    @DisplayName("이름·이메일 동시 수정 → 이력 2건 적재")
    void getHistory_nameAndEmail() throws Exception {
        mockMvc.perform(patch("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "새이름",
                        "email", "new@test.com"
                ))));

        mockMvc.perform(get("/api/auth/me/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(2));
    }

    @Test
    @DisplayName("비밀번호 변경 → 이력 적재 (값은 마스킹)")
    void getHistory_afterPasswordChange() throws Exception {
        mockMvc.perform(patch("/api/auth/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "currentPassword", "Test1234!",
                        "newPassword", "New5678@"
                ))));

        mockMvc.perform(get("/api/auth/me/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].changedField").value("password"))
                .andExpect(jsonPath("$.history[0].oldValue").value(org.hamcrest.Matchers.startsWith("$2a$")))
                .andExpect(jsonPath("$.history[0].newValue").value(org.hamcrest.Matchers.startsWith("$2a$")));
    }

    @Test
    @DisplayName("값이 같으면 이력 적재 안 함")
    void getHistory_sameValueNotRecorded() throws Exception {
        // 동일한 이름으로 수정 요청
        mockMvc.perform(patch("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "테스트유저"))));

        mockMvc.perform(get("/api/auth/me/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(0));
    }
}
