package com.premtsd.linkedin.connections.internal;

import jakarta.persistence.*;
import lombok.*;

/**
 * A user as seen by the connections graph. In the microservices version this was a
 * Neo4j @Node; here it is JPA-modeled for a zero-dependency run. Populated by
 * reacting to the user module's UserRegisteredEvent (same as the microservices
 * connections-service consuming the user-created topic).
 */
@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Person {

    @Id
    private Long userId; // mirrors the user module's id

    private String name;
}
