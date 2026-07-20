package com.premtsd.linkedin.post.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

class PostDtos {

    record CreatePostRequest(
            @NotBlank @Size(max = 2048) String content,
            String imageUrl) {
    }

    record PostView(Long id, Long userId, String content, String imageUrl,
                    Instant createdAt, long likeCount) {
    }
}
