package com.premtsd.linkedin.connections.events;

import org.springframework.modulith.events.Externalized;

/** Exposed event — a connection request was sent. */
@Externalized("connection-requested::#{#this.receiverId().toString()}")
public record ConnectionRequestedEvent(Long senderId, Long receiverId) {
}
