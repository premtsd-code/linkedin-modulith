package com.premtsd.linkedin.post.events;

import org.springframework.modulith.events.Externalized;

/** Exposed event — "someone liked a post". Carries the author so listeners can notify them. */
@Externalized("post-liked::#{#this.authorId().toString()}")
public record PostLikedEvent(Long postId, Long likerId, Long authorId) {
}
