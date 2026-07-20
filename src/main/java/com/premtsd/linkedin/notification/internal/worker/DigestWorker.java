package com.premtsd.linkedin.notification.internal.worker;

import com.premtsd.linkedin.jobs.JobContext;
import com.premtsd.linkedin.jobs.JobRunner;
import com.premtsd.linkedin.notification.internal.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rolls each user's unread notifications into a single digest, one keyset page of users
 * per invocation — the same bounded-lease / resumable shape as the feed workers. Enqueued
 * on a schedule by {@link NotificationDigestSchedule}, executed by the jobs runtime.
 */
@Component
@RequiredArgsConstructor
class DigestWorker implements JobRunner {

    private static final int PAGE_SIZE = 500;

    private final NotificationService notifications;

    @Override
    public String type() {
        return "notification.digest";
    }

    @Override
    public void run(JobContext ctx) {
        long cursor = ctx.checkpointAsLong(0L);

        List<Long> users = notifications.usersWithUnreadAfter(cursor, PAGE_SIZE);
        if (users.isEmpty()) {
            return; // done
        }

        users.forEach(notifications::sendDigest);

        if (users.size() == PAGE_SIZE) {
            ctx.reschedule(users.get(users.size() - 1)); // continue from the last user id
        }
    }
}
