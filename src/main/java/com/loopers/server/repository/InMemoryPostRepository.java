package com.loopers.server.repository;

import com.loopers.server.model.Post;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
public class InMemoryPostRepository {

    private final List<Post> posts = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public List<Post> findAll() {
        return new ArrayList<>(posts);
    }

    public Post findById(int id) {
        return posts.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public List<Post> findByReporterId(int reporterId) {
        return posts.stream()
                .filter(p -> p.getReporterId() == reporterId)
                .collect(Collectors.toList());
    }

    public Post save(Post post) {
        post.setId(idCounter.getAndIncrement());
        posts.add(0, post); // 최신순
        return post;
    }

    public boolean deleteByReporterId(int reporterId) {
        return posts.removeIf(p -> p.getReporterId() == reporterId);
    }

    /** 테스트 격리용 — 전체 데이터 초기화 */
    public void clear() {
        posts.clear();
        idCounter.set(1);
    }
}
