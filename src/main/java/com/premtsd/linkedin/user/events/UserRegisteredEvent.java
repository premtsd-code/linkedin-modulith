package com.premtsd.linkedin.user.events;

/**
 * Exposed event = the user module's public contract for "a user signed up".
 *
 * Lives in the {@code events} named interface so other modules can depend on the
 * event without gaining access to the module's service API (e.g. {@code JwtService}).
 */
public record UserRegisteredEvent(Long userId, String name, String email) {
}
