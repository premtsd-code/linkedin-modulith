package com.premtsd.linkedin.connections;

/** Exposed event — a connection request was sent. */
public record SendConnectionRequestEvent(Long senderId, Long receiverId) {
}
