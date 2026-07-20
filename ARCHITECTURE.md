# Architecture

Diagrams render natively on GitHub (Mermaid). For a quick tour see the [README](README.md);
this doc is the visual reference for how the modules fit together and how work flows.

---

## 1. Module dependency graph

Every arrow is a **declared** `allowedDependencies` edge — `ModularityTests.verify()` fails
the build on any edge not drawn here. Labelled edges go to a module's `events` **named
interface** (the event records only); unlabelled edges use the module's default exposed API.
`platform` and `shared` are `OPEN` (may be depended on freely; not dependency-checked).

```mermaid
flowchart TD
    subgraph business["Business modules"]
        user["user<br/>JwtService · UserRegisteredEvent"]
        post["post<br/>Post/Like · events"]
        connections["connections<br/>ConnectionGraphQuery · events"]
        notification["notification<br/>NotificationWorker · DigestWorker"]
        uploader["uploader<br/>FileStorage"]
        feed["feed<br/>FanoutWorker · FeedTrimWorker"]
    end

    subgraph infra["Platform &amp; infrastructure"]
        jobs["jobs<br/>JobRunner · JobScheduler · JobContext"]
        platform["platform · OPEN<br/>security · web · messaging"]
        shared["shared · OPEN<br/>UserId · PostId · SecurityUtils"]
    end

    post -->|"user::events"| user
    post --> uploader
    connections -->|"user::events"| user
    notification -->|"user::events"| user
    notification -->|"post::events"| post
    notification -->|"connections::events"| connections
    notification --> jobs
    feed -->|"post::events"| post
    feed --> connections
    feed --> jobs

    user --> shared
    post --> shared
    connections --> shared
    notification --> shared
    uploader --> shared
    feed --> shared
    jobs --> shared

    platform -->|"JwtService"| user

    classDef open fill:#eef,stroke:#66f,stroke-dasharray:4 3;
    class platform,shared open;
```

**Reading it:** business modules never reach into each other's internals — they depend on
another module's `events` interface (to react) or its narrow default API (`uploader.FileStorage`,
`connections.ConnectionGraphQuery`). `jobs` is generic and domain-free: things depend on it,
it depends on nothing domain-specific. `platform` (the app shell) is the only thing that
touches `user`'s service API, to validate JWTs.

---

## 2. Event delivery — in-process by default, Kafka at the seam

The **same** `@ApplicationModuleListener` methods handle both paths, so there is one copy of
each rule regardless of how it's deployed.

### Standalone (default, H2, zero dependencies)

```mermaid
flowchart LR
    svc["Module service<br/>publishEvent(...)"] --> reg[("Durable event registry<br/>JPA table")]
    reg -->|"after commit · async · at-least-once"| l["@ApplicationModuleListener<br/>in-process worker"]
    l --> effect["create notification /<br/>project person / enqueue job"]
```

### Split (web + worker, Kafka between them)

```mermaid
flowchart LR
    subgraph web["web role"]
        wsvc["service.publishEvent(...)"] --> wreg[("registry")]
        wreg --> ext["@Externalized<br/>topic::routing-key"]
    end
    ext -->|"produce"| k[("Kafka topic")]
    subgraph worker["worker role"]
        k -->|"consume"| relay["InboundEventRelay<br/>(generic, trusted-packages)"]
        relay -->|"republish in-process"| wl["@ApplicationModuleListener<br/>same worker method"]
    end
```

The web tier only *externalizes*; the worker tier consumes and processes. Workers are gated
`app.role != web`, so a split deployment never double-processes.

---

## 3. Post → feed fanout (why the `jobs` runtime exists)

A post by a well-connected author touches hundreds of thousands of feed rows — too much for a
one-shot listener. The listener just enqueues; the runtime does the chunked, resumable work.

```mermaid
sequenceDiagram
    actor U as Client
    participant P as post · PostService
    participant R as Event registry
    participant L as feed · PostEventListener
    participant J as JobScheduler
    participant PL as JobPoller · worker
    participant F as FanoutWorker
    participant G as connections · ConnectionGraphQuery
    participant DB as feed_entries

    U->>P: POST /api/posts
    P->>R: publish PostCreatedEvent (after commit)
    R-->>L: deliver (async)
    L->>J: enqueueOnce "feed.fanout", key "fanout:postId"
    Note over PL: worker role · every poll-interval
    PL->>PL: claim FOR UPDATE SKIP LOCKED, mark RUNNING
    PL->>F: run(ctx)
    F->>G: connectionIdsAfter(author, cursor, 500)
    G-->>F: page of connection ids
    F->>DB: insertBatch(page) ON CONFLICT DO NOTHING
    alt page full
        F-->>PL: ctx.reschedule(lastId)
        Note over PL: PENDING again, resumes from checkpoint
    else page short
        F-->>PL: done
        PL->>PL: mark COMPLETED
    end
```

`enqueueOnce` (keyed by post id) makes at-least-once delivery idempotent; keyset pagination
(`connectionIdsAfter`) avoids OFFSET degradation; `ON CONFLICT DO NOTHING` makes the resume
overlap a no-op.

---

## 4. Job lifecycle

Claiming is arbitrated entirely by the database — no registry, no leader election. Progress
(`reschedule`) is not a failed attempt; a dead worker's job is recovered by the reaper.

```mermaid
stateDiagram-v2
    [*] --> PENDING: enqueue
    PENDING --> RUNNING: claim SKIP LOCKED, attempts++
    RUNNING --> COMPLETED: run ok
    RUNNING --> PENDING: reschedule, attempts--, checkpoint saved
    RUNNING --> PENDING: threw and attempts < max, backoff
    RUNNING --> FAILED: threw and attempts >= max
    RUNNING --> PENDING: lease expired, reaper requeues
    COMPLETED --> [*]
    FAILED --> [*]
```

---

## 5. Deployment roles

One jar, three roles via `app.role` (the `web` / `worker` Spring profiles set it).

```mermaid
flowchart TB
    jar[["one jar (same image)"]]
    jar --> s["standalone (default)<br/>HTTP + in-process workers<br/>H2 · no Kafka · jobs enqueued only"]
    jar --> w["web<br/>HTTP + externalize to Kafka<br/>no in-process workers"]
    jar --> k["worker<br/>no HTTP<br/>InboundEventRelay + JobPoller"]
    w -->|"Kafka topics"| k
    k -.->|"Postgres · SKIP LOCKED"| db[("jobs / feed tables")]
```

| Role | HTTP | Events | Jobs |
|---|---|---|---|
| `standalone` (default) | yes | in-process, durable registry | enqueued only (H2 can't `SKIP LOCKED`) |
| `web` | yes | externalized to Kafka | enqueued only |
| `worker` | no | consumed from Kafka via relay | poller executes them (Postgres) |
