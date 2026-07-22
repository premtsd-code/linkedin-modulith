package com.premtsd.linkedin.platform.persistence;

/**
 * Tracks which users have written "recently" (within the replication-lag window), so their
 * reads can be pinned to the primary for read-your-writes. Backed by a shared store (Redis)
 * so a write on one app instance is visible to reads served by another.
 */
interface RecentWrites {

    /** Record that {@code userId} just wrote; the mark expires after the configured window. */
    void markWritten(long userId);

    /** True while {@code userId}'s most recent write is still within the window. */
    boolean wroteRecently(long userId);
}
