package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.internal.ConnectionsDtos.PersonView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Graph adapter — the faithful counterpart to the microservices connections-service
 * (which used Neo4j). Uses raw Cypher via Neo4jClient so there is no OGM entity
 * scanning to collide with the JPA modules. Active only under the 'neo4j' profile.
 *
 *   Person nodes; [:REQUESTED] = pending, [:CONNECTED] = accepted (undirected use).
 */
@Repository
@Profile("neo4j")
@RequiredArgsConstructor
class Neo4jConnectionsGraph implements ConnectionsGraph {

    private final Neo4jClient client;

    @Override
    public void registerPerson(Long userId, String name) {
        client.query("MERGE (p:Person {userId: $id}) SET p.name = $name")
                .bind(userId).to("id").bind(name).to("name")
                .run();
    }

    @Override
    public boolean relationshipExists(Long a, Long b) {
        Long count = client.query("""
                        MATCH (x:Person {userId: $a})-[r:REQUESTED|CONNECTED]-(y:Person {userId: $b})
                        RETURN count(r) AS c""")
                .bind(a).to("a").bind(b).to("b")
                .fetchAs(Long.class).mappedBy((t, rec) -> rec.get("c").asLong())
                .one().orElse(0L);
        return count > 0;
    }

    @Override
    public void createPendingRequest(Long senderId, Long receiverId) {
        client.query("""
                        MATCH (a:Person {userId: $s}), (b:Person {userId: $r})
                        MERGE (a)-[:REQUESTED]->(b)""")
                .bind(senderId).to("s").bind(receiverId).to("r")
                .run();
    }

    @Override
    public boolean markAccepted(Long senderId, Long receiverId) {
        Long changed = client.query("""
                        MATCH (a:Person {userId: $s})-[req:REQUESTED]->(b:Person {userId: $r})
                        DELETE req
                        MERGE (a)-[:CONNECTED]->(b)
                        RETURN count(*) AS c""")
                .bind(senderId).to("s").bind(receiverId).to("r")
                .fetchAs(Long.class).mappedBy((t, rec) -> rec.get("c").asLong())
                .one().orElse(0L);
        return changed > 0;
    }

    @Override
    public List<PersonView> firstDegree(Long userId) {
        return client.query("""
                        MATCH (me:Person {userId: $id})-[:CONNECTED]-(o:Person)
                        RETURN o.userId AS userId, o.name AS name""")
                .bind(userId).to("id")
                .fetchAs(PersonView.class)
                .mappedBy((t, rec) -> new PersonView(rec.get("userId").asLong(), rec.get("name").asString()))
                .all().stream().toList();
    }
}
