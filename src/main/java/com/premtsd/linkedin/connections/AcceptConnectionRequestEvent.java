package com.premtsd.linkedin.connections;

/** Exposed event — a connection request was accepted. */
public record AcceptConnectionRequestEvent(Long senderId, Long receiverId) {
}
