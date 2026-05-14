package com.loopers.server.service;

import com.loopers.server.dto.*;
import com.loopers.server.exception.ApiException;
import com.loopers.server.model.User;
import com.loopers.server.model.UserHistory;
import com.loopers.server.repository.InMemoryUserHistoryRepository;
import com.loopers.server.repository.InMemoryUserRepository;
import com.loopers.server.security.JwtUtil;
import com.loopers.server.util.PasswordRotateUtil;
import com.loopers.server.vo.Birthdate;
import com.loopers.server.vo.Email;
import com.loopers.server.vo.LoginId;
import com.loopers.server.vo.Password;
import com.loopers.server.vo.UserName;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final String AGENT_CODE = "Loopers";

    private final InMemoryUserRepository userRepository;
    private final InMemoryUserHistoryRepository historyRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(InMemoryUserRepository userRepository,
                       InMemoryUserHistoryRepository historyRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(RegisterRequest req) {
        // VO 생성 시 각 필드 검증 자동 수행 — 유효하지 않으면 ApiException 발생
        LoginId  loginId   = new LoginId(req.getLoginId());
        UserName name      = new UserName(req.getName());
        Email    email     = new Email(req.getEmail());
        Birthdate birthdate = new Birthdate(req.getBirthdate());
        Password password  = new Password(req.getPassword(), birthdate);

        // 중복 확인
        if (userRepository.findByLoginId(loginId.getValue()) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.findByEmail(email.getValue()) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setLoginId(loginId.getValue());
        user.setName(name.getValue());
        user.setBirthdate(birthdate.getValue());
        user.setEmail(email.getValue());
        user.setPasswordHash(passwordEncoder.encode(password.getValue()));
        user.setDuressPasswordHash(
                req.getDuressPassword() != null && !req.getDuressPassword().isBlank()
                        ? passwordEncoder.encode(req.getDuressPassword())
                        : null
        );
        user.setRole(determineRole(req.getReferral()));

        User saved = userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.generateToken(saved));
        result.put("user", UserResponse.from(saved));
        return result;
    }

    public Map<String, Object> login(LoginRequest req) {
        if (req.getLoginId() == null || req.getPassword() == null
                || req.getLoginId().isBlank() || req.getPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "아이디와 비밀번호를 입력해주세요.");
        }

        User user = userRepository.findByLoginId(req.getLoginId());
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 아이디입니다.");
        }

        // 1) 과거 열쇠 함정
        for (String oldHash : user.getPasswordHistory()) {
            if (passwordEncoder.matches(req.getPassword(), oldHash)) {
                // TODO: 요원 계정이라면 실제 연락처로 경고 알림 발송 (Mocking)
                Map<String, Object> fakeUser = new LinkedHashMap<>();
                fakeUser.put("loginId", "fake_id");
                fakeUser.put("name", "GUEST");
                fakeUser.put("email", "unknown@textfix.kr");
                fakeUser.put("role", "civilian");
                fakeUser.put("isFake", true);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("token", jwtUtil.generateFakeToken());
                result.put("user", fakeUser);
                result.put("trap", "old_key");
                return result;
            }
        }

        // 2) 자폭 비밀번호
        if (user.getDuressPasswordHash() != null
                && passwordEncoder.matches(req.getPassword(), user.getDuressPasswordHash())) {
            userRepository.deleteById(user.getId());
            // TODO: 게시글 등 연관 데이터도 삭제
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("selfDestruct", true);
            return result;
        }

        // 3) 정상 로그인
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다.");
        }

        // 4) 요원이면 로그인 성공 직후 비밀번호 자동 변경
        //    규칙: 비밀번호 내 각 숫자 d → (d+1) % 10
        //    예)  "Pass1239!" → "Pass2340!"
        if ("agent".equals(user.getRole())) {
            String newPlainPassword = PasswordRotateUtil.rotateDigits(req.getPassword());
            String newHash = passwordEncoder.encode(newPlainPassword);

            userRepository.addPasswordHistory(user.getId(), user.getPasswordHash());
            historyRepository.record(user.getId(), "password", user.getPasswordHash(), newHash);
            user.setPasswordHash(newHash);
            userRepository.save(user);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.generateToken(user));
        result.put("user", UserResponse.from(user));
        return result;
    }

    public UserResponse getMe(int userId) {
        User user = userRepository.findById(userId);
        if (user == null) throw new ApiException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(int userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId);
        if (user == null) throw new ApiException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

        // TODO: 이메일 중복 확인 추가
        if (req.getName() != null && !req.getName().isBlank() && !req.getName().equals(user.getName())) {
            historyRepository.record(userId, "name", user.getName(), req.getName());
            user.setName(req.getName());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && !req.getEmail().equals(user.getEmail())) {
            historyRepository.record(userId, "email", user.getEmail(), req.getEmail());
            user.setEmail(req.getEmail());
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    public void changePassword(int userId, ChangePasswordRequest req) {
        if (req.getCurrentPassword() == null || req.getNewPassword() == null
                || req.getCurrentPassword().isBlank() || req.getNewPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호와 새 비밀번호를 입력해주세요.");
        }

        User user = userRepository.findById(userId);
        if (user == null) throw new ApiException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다.");
        }

        // TODO: 비밀번호 유효성 검사 (validatePassword 로직 재사용)

        String newHash = passwordEncoder.encode(req.getNewPassword());
        userRepository.addPasswordHistory(userId, user.getPasswordHash());
        historyRepository.record(userId, "password", user.getPasswordHash(), newHash);
        user.setPasswordHash(newHash);
        userRepository.save(user);
    }

    public Map<String, Object> validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "password 필드가 필요합니다.");
        }
        // 생년월일 없이 형식만 검사 (Password VO의 static validate 사용)
        List<String> errors = Password.validate(password, null);

        Map<String, Object> result = new LinkedHashMap<>();
        if (!errors.isEmpty()) {
            result.put("valid", false);
            result.put("errors", errors);
        } else {
            result.put("valid", true);
        }
        return result;
    }

    public void deleteAccount(int userId) {
        if (!userRepository.deleteById(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        // TODO: 해당 유저의 게시글 등 연관 데이터 삭제
    }

    private String determineRole(String referral) {
        return AGENT_CODE.equals(referral) ? "agent" : "civilian";
    }


    public List<UserHistoryResponse> getMyHistory(int userId) {
        return historyRepository.findByUserId(userId).stream()
                .map(UserHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
