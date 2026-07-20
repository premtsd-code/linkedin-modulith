package com.premtsd.linkedin.post.events;

import org.springframework.modulith.events.Externalized;

/** Exposed event — "a post was created". Routing key is the author id (see @Externalized). */
@Externalized("post-created::#{#this.authorId().toString()}")
public record PostCreatedEvent(Long postId, Long authorId) {
}
