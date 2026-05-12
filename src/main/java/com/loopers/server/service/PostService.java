package com.loopers.server.service;

import com.loopers.server.dto.CreatePostRequest;
import com.loopers.server.dto.PostResponse;
import com.loopers.server.exception.ApiException;
import com.loopers.server.model.Post;
import com.loopers.server.model.User;
import com.loopers.server.repository.InMemoryPostRepository;
import com.loopers.server.repository.InMemoryUserRepository;
import com.loopers.server.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final InMemoryPostRepository postRepository;
    private final InMemoryUserRepository userRepository;

    public PostService(InMemoryPostRepository postRepository, InMemoryUserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public List<PostResponse> getPosts(String subject, String type, UserPrincipal principal) {
        List<Post> posts = postRepository.findAll();

        if (subject != null) {
            posts = posts.stream().filter(p -> subject.equals(p.getSubject())).collect(Collectors.toList());
        }
        if (type != null) {
            posts = posts.stream().filter(p -> type.equals(p.getType())).collect(Collectors.toList());
        }

        return posts.stream().map(p -> toResponse(p, principal)).collect(Collectors.toList());
    }

    public PostResponse getPost(int id, UserPrincipal principal) {
        Post post = postRepository.findById(id);
        if (post == null) throw new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        return toResponse(post, principal);
    }

    public PostResponse createPost(CreatePostRequest req, UserPrincipal principal) {
        if (req.getSubject() == null || req.getContent() == null || req.getPage() == null
                || req.getSubject().isBlank() || req.getContent().isBlank() || req.getPage().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "필수 항목을 입력해주세요.");
        }

        Post post = new Post();
        post.setSubject(req.getSubject());
        post.setGrade(req.getGrade());
        post.setPublisher(req.getPublisher());
        post.setPage(req.getPage());
        post.setType(req.getType());
        post.setContent(req.getContent());
        post.setSecretContent(req.getSecretContent() != null ? req.getSecretContent() : "");
        post.setReporterId(principal.getId());
        post.setLikes(0);
        post.setCreatedAt(LocalDate.now().toString());

        Post saved = postRepository.save(post);
        return toResponse(saved, principal);
    }

    public int likePost(int id) {
        Post post = postRepository.findById(id);
        if (post == null) throw new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        // TODO: 중복 추천 방지
        post.setLikes(post.getLikes() + 1);
        return post.getLikes();
    }

    public Map<String, Object> getRanking() {
        List<Post> allPosts = postRepository.findAll();

        List<PostResponse> byLikes = allPosts.stream()
                .sorted((a, b) -> b.getLikes() - a.getLikes())
                .map(p -> toResponse(p, null))
                .collect(Collectors.toList());

        Map<String, Long> byCountMap = new LinkedHashMap<>();
        for (Post p : allPosts) {
            User user = userRepository.findById(p.getReporterId());
            String key = user != null ? user.getLoginId() : "알 수 없음";
            byCountMap.merge(key, 1L, Long::sum);
        }
        List<Map<String, Object>> reporters = byCountMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("loginId", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byLikes", byLikes);
        result.put("reporters", reporters);
        return result;
    }

    private PostResponse toResponse(Post post, UserPrincipal principal) {
        User reporter = userRepository.findById(post.getReporterId());
        String loginId = reporter != null ? reporter.getLoginId() : "-";
        String name    = reporter != null ? reporter.getName()    : "알 수 없음";

        // 감시 대상 유저에게는 가짜 내용 반환
        if (principal != null && "watchlisted_agent".equals(principal.getRole())) {
            post = cloneWithFakeContent(post);
        }

        return PostResponse.from(post, loginId, name);
    }

    private Post cloneWithFakeContent(Post original) {
        Post fake = new Post();
        fake.setId(original.getId());
        fake.setSubject(original.getSubject());
        fake.setGrade(original.getGrade());
        fake.setPublisher(original.getPublisher());
        fake.setPage(original.getPage());
        fake.setType(original.getType());
        fake.setContent("[허위 정보] 내용이 검열되었습니다.");
        fake.setSecretContent("⚠️ 허위 지령");
        fake.setReporterId(original.getReporterId());
        fake.setLikes(original.getLikes());
        fake.setCreatedAt(original.getCreatedAt());
        return fake;
    }
}
