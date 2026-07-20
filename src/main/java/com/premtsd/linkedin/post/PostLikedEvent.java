package com.premtsd.linkedin.post;

/** Exposed event — "someone liked a post". Carries the author so listeners can notify them. */
public record PostLikedEvent(Long postId, Long likerId, Long authorId) {
}
