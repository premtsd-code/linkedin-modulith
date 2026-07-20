package com.premtsd.linkedin.feed.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface FeedEntryRepository extends JpaRepository<FeedEntry, Long> {

    /**
     * Insert one post into many owners' feeds in a single round-trip. Postgres-only
     * ({@code unnest} + {@code ON CONFLICT}); fanout runs on the worker/Postgres tier.
     * ON CONFLICT DO NOTHING makes the resume-overlap page a no-op — no duplicates.
     */
    @Modifying
    @Query(value = """
            INSERT INTO feed_entries (owner_id, post_id, author_id, created_at)
            SELECT unnest(:ownerIds), :postId, :authorId, now()
            ON CONFLICT (owner_id, post_id) DO NOTHING
            """, nativeQuery = true)
    void insertBatch(@Param("ownerIds") Long[] ownerIds,
                     @Param("postId") long postId,
                     @Param("authorId") long authorId);

    /** Delete up to {@code limit} feed rows older than {@code cutoff}; returns the count deleted. */
    @Modifying
    @Query(value = """
            DELETE FROM feed_entries
            WHERE id IN (SELECT id FROM feed_entries WHERE created_at < :cutoff ORDER BY id LIMIT :limit)
            """, nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") Instant cutoff, @Param("limit") int limit);
}
