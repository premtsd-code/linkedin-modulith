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
| Kafka topics + `@KafkaListener` everywhere | **In-process events** + `@ApplicationModuleListener` (durable, restart-safe), Kafka only at the web↔worker seam |
| Duplicated event DTO packages across services | **One event type per fact**, exposed via a module's `events` named interface |
| Feign clients between services | Direct calls to a module's exposed API |
| Gateway JWT filter → spoofable `X-User-Id` header | **One Spring Security context** (no trusted header) |
| Client-supplied roles at signup (privilege escalation) | **Server-assigned roles** — bug fixed |
| Boundaries by hope | Boundaries **verified** by `ModularityTests` (`allowedDependencies` + ArchUnit) |

## Modules

Each module hides its implementation in an `internal/` sub-package; only the module
root (and its `events` named interface) is visible to other modules. Every
`package-info.java` declares its `allowedDependencies`, and `ModularityTests.verify()`
fails the build on any reach past them.

**Business modules**

- `user` — accounts + auth. Owns the `User` aggregate. Exposes `JwtService`; publishes `UserRegisteredEvent`.
- `post` — posts, likes, feed-of-one. Publishes `PostCreatedEvent` / `PostLikedEvent`. Calls `uploader` directly for image storage.
- `connections` — the symmetric connection graph (JPA by default, Neo4j under the `neo4j` profile). Publishes `ConnectionRequestedEvent` / `ConnectionAcceptedEvent`; exposes `ConnectionGraphQuery.connectionIdsAfter(...)` (keyset pagination for fanout).
- `notification` — in-app notifications. One `NotificationWorker` reacts to all five events; a scheduled `DigestWorker` (a `JobRunner`) rolls each user's unread notifications into a periodic digest.
- `uploader` — file storage behind a `FileStorage` port (local disk, or Cloudinary under the `cloudinary` profile).
- `feed` — materializes each post into its author's connections' feeds. The one workload that genuinely can't be a one-shot listener, so it runs on `jobs`.

**Platform / infrastructure modules**

- `jobs` — a generic, domain-free **durable job runtime** (see below). `OPEN`-adjacent: domain modules depend on it; it depends on nothing domain-specific.
- `platform` (`OPEN`) — cross-cutting technical wiring: `security` (JWT filter + Spring Security chain), `web` (global error handling), `messaging` (Kafka topic provisioning + the inbound event relay).
- `shared` (`OPEN`) — pure value types (`UserId`, `PostId`) + `SecurityUtils` (the in-process replacement for the gateway's `X-User-Id` header). Kept framework-free and guarded by `SharedModulePurityTests`.

## Events: in-process by default, Kafka at the seam

Modules never call each other's services to notify — they publish events. Delivery is
Spring Modulith's **durable event registry** (a JPA table): `@ApplicationModuleListener`
handlers run **async, after commit**, and are re-delivered at-least-once if a handler
fails or the app restarts — exactly like a Kafka consumer, with no broker.

To scale workers as a **separate process**, each event is `@Externalized` to Kafka on the
web tier, and a single generic `InboundEventRelay` republishes them as in-process events
on the worker tier — so the *same* `@ApplicationModuleListener` methods handle both paths.
One copy of each rule, no drift.

## The `jobs` runtime

For work that can't be a one-shot listener — fanout to a large connection set, chunked or
scheduled work needing retries. Public API is three types: `JobRunner`, `JobScheduler`,
`JobContext`.

- **Claiming is arbitrated by the database** — `SELECT … FOR UPDATE SKIP LOCKED`, no
  registry or leader election.
- **Resumable** — a `JobContext` checkpoint survives across attempts; `ctx.reschedule(...)`
  continues a long job one bounded slice at a time (progress is not a failed attempt).
- **Self-healing** — a reaper returns jobs whose worker died (expired lease) to `PENDING`.
- **Idempotent enqueue** — `enqueueOnce(type, dedupeKey, …)` for at-least-once event
  delivery and cron firings.

`FanoutWorker` is the flagship: keyset pagination, one page per invocation (bounded lease),
resumable via checkpoint, idempotent via `ON CONFLICT DO NOTHING`.

> **Postgres-only.** `FOR UPDATE SKIP LOCKED` doesn't exist in H2, so jobs execute only
> in the worker role on Postgres. In the default H2/standalone mode jobs are *enqueued*
> but not run — the rest of the app is unaffected.

## Deployment roles

One jar, three roles via `app.role` (the `web` / `worker` Spring profiles set it):

| Role | HTTP | Events | Jobs |
|---|---|---|---|
| `standalone` (default) | yes | in-process, durable registry | enqueued only (H2) |
| `web` | yes | externalized to Kafka | enqueued only |
| `worker` | no | consumed from Kafka via relay | poller executes them |

```bash
mvn spring-boot:run                                  # standalone, H2, zero dependencies
java -jar app.jar --spring.profiles.active=web       # HTTP + externalize to Kafka
java -jar app.jar --spring.profiles.active=worker    # no HTTP; relay + job poller
```

Other profiles: `postgres` (real DB), `neo4j` (graph-backed connections), `cloudinary` (cloud uploads).

## Run it

Zero external dependencies (H2 in-memory by default):

```bash
mvn spring-boot:run
```

```bash
# 1. Sign up -> returns a JWT; a worker asynchronously creates a welcome notification
TOKEN=$(curl -s localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@example.com","password":"secret123"}' | jq -r .token)

# 2. Create a post (publishes PostCreatedEvent -> feed fanout is enqueued)
curl -s localhost:8080/api/posts -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"content":"Hello, modulith!"}'

# 3. Read your notifications
curl -s localhost:8080/api/notifications -H "Authorization: Bearer $TOKEN"
```

Endpoints: `/api/auth/**`, `/api/posts/**`, `/api/connections/**`, `/api/notifications`, `/api/uploads`.

## Verify the architecture

```bash
mvn test
```

- `ModularityTests` — `verify()` fails the build on any boundary violation, plus ArchUnit
  rules that keep JobRunners in `internal.worker` and repositories internal.
- `SharedModulePurityTests` — fails the build if `shared` grows a Spring stereotype,
  `@Configuration`, `@Entity`, repository or controller. The one rule `verify()` can't
  catch, so it gets its own guard (verified to fail on a planted `@Service`).
- `AuthFlowTests` / `NotificationWorkerTests` — end-to-end HTTP + async worker on H2.
- `JobRuntimeTests` — the jobs runtime against real Postgres via Testcontainers
  (execution, `enqueueOnce` idempotence, retry-with-backoff, checkpoint resume).
  **Skipped automatically when Docker is absent.**

Module docs + PlantUML diagrams are generated under `target/spring-modulith-docs/`.
