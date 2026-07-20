package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.ConnectionGraphQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Relational adapter for {@link ConnectionGraphQuery}, over the {@code connection_requests}
 * table. (The Neo4j profile stores the graph elsewhere; feed fanout targets the
 * relational/Postgres deployment.)
 */
@Component
@RequiredArgsConstructor
class JpaConnectionGraphQuery implements ConnectionGraphQuery {

    private final ConnectionRequestRepository requests;

    @Override
    public List<Long> followerIdsAfter(Long userId, long afterId, int limit) {
        return requests.findFollowerIdsAfter(userId, afterId, limit);
    }
}
