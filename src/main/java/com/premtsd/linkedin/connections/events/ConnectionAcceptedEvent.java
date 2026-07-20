package com.premtsd.linkedin.connections.events;

/** Exposed event — a connection request was accepted. */
public record ConnectionAcceptedEvent(Long senderId, Long receiverId) {
}
