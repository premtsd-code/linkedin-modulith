package com.premtsd.linkedin.jobs;

import java.util.UUID;

/**
 * Execution handle passed to {@link JobRunner#run}. Carries the immutable payload plus
 * a mutable checkpoint that survives across attempts, so long work resumes rather than
 * restarts.
 */
public interface JobContext {

    /** This job's id. */
    UUID jobId();

    /** Read a payload argument, converted to the requested type. */
    <T> T arg(String key, Class<T> type);

    /** The current checkpoint parsed as a long, or {@code defaultValue} if unset. */
    long checkpointAsLong(long defaultValue);

    /** The raw checkpoint string, or null if unset. */
    String checkpoint();

    /**
     * Record progress and ask the runtime to run this job again with the given checkpoint.
     * Does NOT burn an attempt — progress is not failure.
     */
    void reschedule(Object nextCheckpoint);

    /** Extend the lease mid-slice, for a run() that legitimately outlives one lease period. */
    void heartbeat();
}
