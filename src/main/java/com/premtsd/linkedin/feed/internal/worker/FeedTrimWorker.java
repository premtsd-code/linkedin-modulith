package com.premtsd.linkedin.feed.internal.worker;

import com.premtsd.linkedin.feed.internal.persistence.FeedEntryRepository;
import com.premtsd.linkedin.jobs.JobContext;
import com.premtsd.linkedin.jobs.JobRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Retention trim: deletes feed rows older than the retention window in bounded batches,
 * rescheduling until a batch comes back short. Same bounded-lease / resumable shape as
 * {@link FanoutWorker}. Enqueued nightly by {@link FeedMaintenanceSchedule}.
 */
@Component
@RequiredArgsConstructor
class FeedTrimWorker implements JobRunner {

    private static final Duration RETENTION = Duration.ofDays(90);
    private static final int BATCH = 1000;

    private final FeedEntryRepository feed;

    @Override
    public String type() {
        return "feed.trim";
    }

    @Override
    public void run(JobContext ctx) {
        Instant cutoff = Instant.now().minus(RETENTION);
        int deleted = feed.deleteOlderThan(cutoff, BATCH);
        if (deleted == BATCH) {
            ctx.reschedule("more"); // more to delete; run again (checkpoint unused here)
        }
    }
}
