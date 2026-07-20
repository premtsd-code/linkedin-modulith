package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.internal.ConnectionsDtos.PersonView;

import java.util.List;

/**
 * Port for the connections graph. Two adapters implement it:
 *   - JpaConnectionsGraph   (default) — relational tables
 *   - Neo4jConnectionsGraph ('neo4j' profile) — a real graph store, faithful to
 *     the microservices connections-service
 *
 * ConnectionsService (rules + events) depends only on this port, so the storage
 * engine swaps with a profile and no business-logic change.
 */
interface ConnectionsGraph {

    void registerPerson(Long userId, String name);

    /** True if any request/connection already exists between the two, either direction. */
    boolean relationshipExists(Long a, Long b);

    void createPendingRequest(Long senderId, Long receiverId);

    /** Accept a pending request; returns true only if a pending request was actually transitioned. */
    boolean markAccepted(Long senderId, Long receiverId);

    List<PersonView> firstDegree(Long userId);
}
