package com.premtsd.linkedin.notification.internal.worker;

import com.premtsd.linkedin.jobs.JobScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Enqueues the daily notification digest rather than running it inline, so the scheduled
 * method stays instant and the work gets the full retry machinery. Date-stamped dedupe key
 * means every worker replica firing the same cron produces exactly one job. Worker role only.
 */
@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
@RequiredArgsConstructor
class NotificationDigestSchedule {

    private final JobScheduler jobs;

    @Scheduled(cron = "${notification.digest-cron:0 0 8 * * *}")
    void dailyDigest() {
        jobs.enqueueOnce("notification.digest", "digest:" + LocalDate.now(), Map.of());
    }
}
