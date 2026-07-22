package com.premtsd.linkedin.feed.internal.worker;

import com.premtsd.linkedin.connections.ConnectionGraphQuery;
import com.premtsd.linkedin.feed.internal.persistence.FeedStore;
import com.premtsd.linkedin.jobs.JobContext;
import com.premtsd.linkedin.jobs.JobRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The reason the jobs module exists. Fans one post out to the author's connections, one
 * bounded page per invocation:
 *   - bounded lease  — one call handles PAGE_SIZE owners, so the lease is sized to a page;
 *   - resumable      — a crash mid-fanout is returned to PENDING by the reaper with the
 *                      checkpoint (last owner id) intact;
 *   - idempotent     — the overlapping page re-inserts under ON CONFLICT DO NOTHING.
 * Keyset pagination, not OFFSET — OFFSET degrades linearly, and fanout is where that bites.
 */
@Component
@RequiredArgsConstructor
class FanoutWorker implements JobRunner {

    private static final int PAGE_SIZE = 500;

    private final ConnectionGraphQuery graph;
    private final FeedStore feed;

    @Override
    public String type() {
        return "feed.fanout";
    }

    @Override
    public int maxAttempts() {
        return 8;
    }

    @Override
    public void run(JobContext ctx) {
        long postId = ctx.arg("postId", Long.class);
        long authorId = ctx.arg("authorId", Long.class);
        long cursor = ctx.checkpointAsLong(0L);

        List<Long> page = graph.connectionIdsAfter(authorId, cursor, PAGE_SIZE);
        if (page.isEmpty()) {
            return; // done
        }

        feed.insertBatch(page, postId, authorId);

        if (page.size() == PAGE_SIZE) {
            ctx.reschedule(page.get(page.size() - 1)); // continue from the last owner id
        }
    }
}
