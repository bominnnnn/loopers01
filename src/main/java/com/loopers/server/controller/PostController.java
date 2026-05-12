package com.loopers.server.controller;

import com.loopers.server.dto.CreatePostRequest;
import com.loopers.server.dto.PostResponse;
import com.loopers.server.security.UserPrincipal;
import com.loopers.server.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** GET /api/posts — 게시글 목록 조회 */
    @GetMapping
    public ResponseEntity<Map<String, List<PostResponse>>> getPosts(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("posts", postService.getPosts(subject, type, principal)));
    }

    /** GET /api/posts/ranking/all — 랭킹 조회 (/:id 보다 먼저 선언해야 함) */
    @GetMapping("/ranking/all")
    public ResponseEntity<Map<String, Object>> getRanking() {
        return ResponseEntity.ok(postService.getRanking());
    }

    /** GET /api/posts/:id — 게시글 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, PostResponse>> getPost(
            @PathVariable int id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("post", postService.getPost(id, principal)));
    }

    /** POST /api/posts — 게시글 작성 */
    @PostMapping
    public ResponseEntity<Map<String, PostResponse>> createPost(
            @RequestBody CreatePostRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("post", postService.createPost(req, principal)));
    }

    /** POST /api/posts/:id/like — 게시글 추천 */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Integer>> likePost(@PathVariable int id) {
        return ResponseEntity.ok(Map.of("likes", postService.likePost(id)));
    }
}
