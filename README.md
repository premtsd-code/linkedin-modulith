# LinkedIn — Modular Monolith (Spring Modulith)

The **modulith counterpart** to the [`linkedIn`](../linkedIn) microservices project.
Same domain, same Spring Boot 3.3 / Java 21 stack — but one deployable, with module
boundaries enforced at build time instead of at the network.

Keeping both side by side is the point: it makes the microservices-vs-modulith
trade-off concrete and demonstrable.

## Why this exists / what it demonstrates

| Microservices version | Modulith version |
|---|---|
| 8 services + gateway + Eureka + Config Server | **1 deployable**, modules = packages |
| Kafka topics + `@KafkaListener` | **In-process events** + `@ApplicationModuleListener` (durable, restart-safe) |
| Duplicated event DTO packages across services | **One shared event type** (the module's public API) |
| Feign clients between services | Direct calls to a module's exposed API |
| Gateway JWT filter → spoofable `X-User-Id` header | **One Spring Security context** (no trusted header) |
| Client-supplied roles at signup (privilege escalation) | **Server-assigned roles** — bug fixed |
| Boundaries by hope | Boundaries **verified** by `ModularityTests` |

## Modules

- `user` — accounts + auth. Owns the `User` aggregate. Exposes `JwtService` and
  publishes `UserRegisteredEvent`.
- `notification` — in-app notifications. Contains **the worker** (`UserEventWorker`)
  that reacts to `UserRegisteredEvent`.

Everything else (`SecurityConfig`, `JwtAuthFilter`, `AppConfig`) is the application
**shell** in the root package — cross-cutting wiring, not a business module.

_Roadmap: `post` (Postgres, partitioned), `connections` (Neo4j), `uploader`, plus a
GraphQL API over the module APIs._

## Workers (Appwrite-style)

A "worker" here is an `@ApplicationModuleListener` — it runs **async**, **after commit**,
off Spring Modulith's **durable event registry** (a JPA table), so failed/interrupted
handlers are re-delivered at-least-once, exactly like a Kafka consumer.

To run workers as a **separate, independently-scaled process** (the Appwrite model of
one image in many roles), deploy the same jar twice:

```bash
java -jar app.jar --spring.profiles.active=web      # HTTP only
java -jar app.jar --spring.profiles.active=worker   # async listeners only
```

## Run it

Zero external dependencies (H2 in-memory by default):

```bash
mvn spring-boot:run
```

```bash
# 1. Sign up -> returns a JWT; the worker asynchronously creates a welcome notification
curl -s localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@example.com","password":"secret123"}'

# 2. Read your notifications (use the token from step 1)
curl -s localhost:8080/api/notifications -H "Authorization: Bearer <TOKEN>"
```

Use `--spring.profiles.active=postgres` to point at a real Postgres instead of H2.

## Verify the architecture

```bash
mvn test        # ModularityTests.verify() fails the build on any boundary violation
```

Module docs + PlantUML diagrams are generated under `target/spring-modulith-docs/`.
