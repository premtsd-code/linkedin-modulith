package com.premtsd.linkedin.connections.events;

import org.springframework.modulith.events.Externalized;

/** Exposed event — a connection request was accepted. */
@Externalized("connection-accepted::#{#this.senderId().toString()}")
public record ConnectionAcceptedEvent(Long senderId, Long receiverId) {
}
