package com.premtsd.linkedin.post.events;

/** Exposed event — "a post was created". Routing key is the author id (see @Externalized). */
public record PostCreatedEvent(Long postId, Long authorId) {
}
