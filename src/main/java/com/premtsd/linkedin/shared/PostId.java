package com.premtsd.linkedin.shared;

/**
 * Typed identifier for a post. A pure value type — no Spring, no persistence. See
 * {@link UserId}.
 */
public record PostId(Long value) {

    public PostId {
        if (value == null) {
            throw new IllegalArgumentException("postId must not be null");
        }
    }

    public static PostId of(Long value) {
        return new PostId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
