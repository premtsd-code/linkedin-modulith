package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.ConnectionGraphQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Relational adapter for {@link ConnectionGraphQuery} (default), over the
 * {@code connection_requests} table. Active unless the 'neo4j' profile is on — under
 * that profile the graph lives in Neo4j and {@link Neo4jConnectionGraphQuery} answers
 * fanout instead, so the two stores never disagree about who a user is connected to.
 */
@Component
@Profile("!neo4j")
@RequiredArgsConstructor
class JpaConnectionGraphQuery implements ConnectionGraphQuery {

    private final ConnectionRequestRepository requests;

    @Override
    public List<Long> connectionIdsAfter(Long userId, long afterId, int limit) {
        return requests.findConnectionIdsAfter(userId, afterId, limit);
    }
}
