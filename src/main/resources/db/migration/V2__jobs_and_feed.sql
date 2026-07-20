-- Postgres schema for the jobs runtime and the feed module.
--
-- Applied by Flyway on a Postgres deployment (with a V1 baseline of the entity
-- tables). Disabled by default: the H2 single-process mode uses Hibernate ddl-auto,
-- which creates the tables but NOT these partial indexes — and the partial indexes
-- are the point: they keep the claim path fast and enforce enqueueOnce.

CREATE TABLE IF NOT EXISTS jobs (
    id           UUID PRIMARY KEY,
    type         VARCHAR(255) NOT NULL,
    payload      TEXT,
    checkpoint   TEXT,
    status       VARCHAR(16)  NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    max_attempts INT          NOT NULL DEFAULT 5,
    run_at       TIMESTAMPTZ  NOT NULL,
    lease_until  TIMESTAMPTZ,
    worker_id    VARCHAR(255),
    dedupe_key   VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ
);

-- Claim path: PENDING jobs due to run, ordered by run_at. Partial so the index stays
-- tiny — finished jobs (the vast majority over time) are excluded.
CREATE INDEX IF NOT EXISTS idx_jobs_claim ON jobs (run_at) WHERE status = 'PENDING';

-- Reaper path: RUNNING jobs whose lease has expired.
CREATE INDEX IF NOT EXISTS idx_jobs_lease ON jobs (lease_until) WHERE status = 'RUNNING';

-- enqueueOnce enforcement. DefaultJobScheduler does a read-then-write that races;
-- THIS partial unique index is the actual guarantee of one live job per dedupe key.
CREATE UNIQUE INDEX IF NOT EXISTS uq_jobs_dedupe_live ON jobs (dedupe_key)
    WHERE dedupe_key IS NOT NULL AND status IN ('PENDING', 'RUNNING');

CREATE TABLE IF NOT EXISTS feed_entries (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id   BIGINT NOT NULL,
    post_id    BIGINT NOT NULL,
    author_id  BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Fanout idempotency: the resume-overlap page re-inserts under ON CONFLICT DO NOTHING.
CREATE UNIQUE INDEX IF NOT EXISTS uq_feed_owner_post ON feed_entries (owner_id, post_id);
