package com.premtsd.linkedin.post.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
}
