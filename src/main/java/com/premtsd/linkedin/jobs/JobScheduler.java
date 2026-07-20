package com.premtsd.linkedin.jobs;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The enqueue side of the runtime — the only way domain modules submit work. Callers
 * enqueue and return; execution happens on the worker tier via the poller.
 */
public interface JobScheduler {

    /** Enqueue to run as soon as a worker is free. */
    UUID enqueue(String type, Map<String, Object> payload);

    /** Enqueue to run at (or after) the given instant. */
    UUID enqueueAt(String type, Map<String, Object> payload, Instant runAt);

    /** Enqueue to run after the given delay. */
    UUID enqueueAfter(String type, Map<String, Object> payload, Duration delay);

    /**
     * Enqueue at most one live job for {@code dedupeKey}. If a PENDING/RUNNING job with
     * the same key already exists, no new job is created and its id is returned. Used to
     * make at-least-once event delivery and fan-out cron firings idempotent.
     */
    UUID enqueueOnce(String type, String dedupeKey, Map<String, Object> payload);

    /** Cancel a job if it is still PENDING. Returns false if it was missing or already running/done. */
    boolean cancel(UUID jobId);
}
