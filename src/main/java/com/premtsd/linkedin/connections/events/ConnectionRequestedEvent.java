package com.premtsd.linkedin.connections.events;

/** Exposed event — a connection request was sent. */
public record ConnectionRequestedEvent(Long senderId, Long receiverId) {
}
