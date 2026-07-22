package com.premtsd.linkedin.feed.internal.persistence;

import java.time.Instant;
import java.util.List;

/**
 * Port for the materialized feed, implemented by {@link JpaFeedStore} — relational rows in
 * Postgres/H2. Under the 'feeddb' profile those rows live in a dedicated Postgres (DB B),
 * otherwise in the primary database; either way the feed workers ({@code FanoutWorker},
 * {@code FeedTrimWorker}) depend only on this port. Both operations are bounded (one page /
 * one batch) and idempotent, so a resumed job never duplicates.
 */
public interface FeedStore {

    /**
     * Insert one post into many owners' feeds. Idempotent: an owner that already has this
     * post is left untouched (relational {@code ON CONFLICT DO NOTHING}), so the overlapping
     * page after a resume re-inserts harmlessly.
     */
    void insertBatch(List<Long> ownerIds, long postId, long authorId);

    /** Delete up to {@code limit} feed entries older than {@code cutoff}; returns the count deleted. */
    int deleteOlderThan(Instant cutoff, int limit);
}
