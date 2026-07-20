package com.premtsd.linkedin.post.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
}
