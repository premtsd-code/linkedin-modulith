package com.premtsd.linkedin.post;

/** Exposed event — "a post was created". */
public record PostCreatedEvent(Long postId, Long authorId, String content) {
}
