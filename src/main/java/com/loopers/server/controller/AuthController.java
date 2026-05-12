package com.loopers.server.controller;

import com.loopers.server.dto.*;
import com.loopers.server.dto.UserHistoryResponse;
import com.loopers.server.security.UserPrincipal;
import com.loopers.server.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/register — 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    /** POST /api/auth/login — 로그인 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** GET /api/auth/me — 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getMe(principal.getId()));
    }

    /** PATCH /api/auth/me — 내 정보 수정 */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(authService.updateProfile(principal.getId(), req));
    }

    /** PATCH /api/auth/me/password — 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChangePasswordRequest req) {
        authService.changePassword(principal.getId(), req);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }

    /** POST /api/auth/validate-password — 비밀번호 유효성 검사 */
    @PostMapping("/validate-password")
    public ResponseEntity<Map<String, Object>> validatePassword(@RequestBody ValidatePasswordRequest req) {
        Map<String, Object> result = authService.validatePassword(req.getPassword());
        boolean valid = (boolean) result.get("valid");
        return ResponseEntity.status(valid ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }

    /** DELETE /api/auth/me — 회원 탈퇴 */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        authService.deleteAccount(principal.getId());
        return ResponseEntity.ok(Map.of("message", "계정이 삭제되었습니다."));
    }

    /** GET /api/auth/me/history — 내 정보 수정 이력 조회 */
    @GetMapping("/me/history")
    public ResponseEntity<Map<String, Object>> getMyHistory(@AuthenticationPrincipal UserPrincipal principal) {
        List<UserHistoryResponse> history = authService.getMyHistory(principal.getId());
        return ResponseEntity.ok(Map.of("history", history));
    }

    /** GET /api/auth/me/posts — 내가 작성한 게시글 목록 */
    @GetMapping("/me/posts")
    public ResponseEntity<Map<String, Object>> getMyPosts() {
        // TODO: PostService 에서 userId 로 필터링
        return ResponseEntity.ok(Map.of("posts", List.of()));
    }
}
