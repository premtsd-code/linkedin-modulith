package com.premtsd.linkedin.jobs;

/**
 * A unit of work the runtime can execute. Implementations are discovered as Spring
 * beans and keyed by {@link #type()}. A single {@link #run(JobContext)} invocation
 * should handle one bounded slice of work and use {@link JobContext#reschedule} to
 * continue, so the lease is sized to a slice rather than the whole job.
 */
public interface JobRunner {

    /** Stable job type key, e.g. {@code "feed.fanout"}. Must be unique across runners. */
    String type();

    /** Execute one slice. Throwing triggers retry-with-backoff up to {@link #maxAttempts()}. */
    void run(JobContext ctx) throws Exception;

    /** Maximum attempts before the job is parked as FAILED. */
    default int maxAttempts() {
        return 5;
    }
}
