package com.premtsd.linkedin.feed.internal.worker;

import com.premtsd.linkedin.jobs.JobScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Cron triggers enqueue rather than execute, so the scheduled method stays instant and
 * the work gets the full retry machinery. The dedupe key is date-stamped, so every worker
 * replica firing the same cron produces exactly one job. Worker role only.
 */
@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
@RequiredArgsConstructor
class FeedMaintenanceSchedule {

    private final JobScheduler jobs;

    @Scheduled(cron = "${feed.trim-cron:0 30 3 * * *}")
    void nightlyTrim() {
        jobs.enqueueOnce("feed.trim", "feed-trim:" + LocalDate.now(), Map.of());
    }
}
