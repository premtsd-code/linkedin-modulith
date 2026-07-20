package com.premtsd.linkedin.user;

/**
 * Exposed event = the user module's public contract for "a user signed up".
 *
 * This is the in-process replacement for the Kafka {@code user-created-topic} /
 * {@code userCreatedTopic} messages. Other modules (e.g. notification) react to
 * it via {@code @ApplicationModuleListener}. Because there is ONE shared type,
 * the copy-paste event-DTO duplication from the microservices version is gone.
 */
public record UserRegisteredEvent(Long userId, String name, String email) {
}
