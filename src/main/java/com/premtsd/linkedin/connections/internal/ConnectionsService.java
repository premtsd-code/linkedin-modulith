package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.events.ConnectionAcceptedEvent;
import com.premtsd.linkedin.connections.events.ConnectionRequestedEvent;

import com.premtsd.linkedin.connections.internal.ConnectionsDtos.PersonView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business rules + event publishing. Storage is delegated to the {@link ConnectionsGraph}
 * port, so this class is identical whether the graph is JPA- or Neo4j-backed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class ConnectionsService {

    private final ConnectionsGraph graph;
    private final ApplicationEventPublisher events;

    @Transactional
    void sendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot connect to yourself");
        }
        if (graph.relationshipExists(senderId, receiverId)) {
            throw new IllegalArgumentException("A request already exists between these users");
        }
        graph.createPendingRequest(senderId, receiverId);
        log.info("Connection request {} -> {}", senderId, receiverId);
        events.publishEvent(new ConnectionRequestedEvent(senderId, receiverId));
    }

    @Transactional
    void acceptRequest(Long receiverId, Long senderId) {
        if (!graph.markAccepted(senderId, receiverId)) {
            throw new IllegalArgumentException("No pending request from " + senderId);
        }
        log.info("Connection accepted {} <-> {}", senderId, receiverId);
        events.publishEvent(new ConnectionAcceptedEvent(senderId, receiverId));
    }

    @Transactional(readOnly = true)
    List<PersonView> firstDegree(Long userId) {
        return graph.firstDegree(userId);
    }

    @Transactional
    void registerPerson(Long userId, String name) {
        graph.registerPerson(userId, name);
    }
}
