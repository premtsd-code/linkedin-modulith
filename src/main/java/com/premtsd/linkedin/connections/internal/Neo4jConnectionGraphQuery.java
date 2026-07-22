package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.ConnectionGraphQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Graph adapter for {@link ConnectionGraphQuery} ('neo4j' profile). Keyset page of a user's
 * accepted connections straight from the graph, so feed fanout reads the same store
 * {@link Neo4jConnectionsGraph} writes to. Traverses {@code [:CONNECTED]} undirected, mirroring
 * {@code firstDegree}; {@code o.userId > $after} + {@code ORDER BY} give the keyset window.
 */
@Component
@Profile("neo4j")
@RequiredArgsConstructor
class Neo4jConnectionGraphQuery implements ConnectionGraphQuery {

    private final Neo4jClient client;

    @Override
    public List<Long> connectionIdsAfter(Long userId, long afterId, int limit) {
        return client.query("""
                        MATCH (me:Person {userId: $id})-[:CONNECTED]-(o:Person)
                        WHERE o.userId > $after
                        RETURN o.userId AS userId
                        ORDER BY o.userId
                        LIMIT $limit""")
                .bind(userId).to("id")
                .bind(afterId).to("after")
                .bind(limit).to("limit")
                .fetchAs(Long.class).mappedBy((t, rec) -> rec.get("userId").asLong())
                .all().stream().toList();
    }
}
