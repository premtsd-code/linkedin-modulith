package com.premtsd.linkedin.shared;

/**
 * Typed identifier for a user. A pure value type — no Spring, no persistence — so it can
 * be shared by every module without dragging framework dependencies across boundaries.
 * Adoption into entities/events is opt-in; the modules currently pass raw {@code Long} ids.
 */
public record UserId(Long value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
