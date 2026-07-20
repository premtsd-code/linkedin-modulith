package com.premtsd.linkedin.feed.internal.worker;

import com.premtsd.linkedin.jobs.JobScheduler;
import com.premtsd.linkedin.post.events.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The seam between events and jobs: a new post enqueues a fanout job and returns.
 * enqueueOnce (keyed by post id) because event delivery is at-least-once — a redelivered
 * PostCreatedEvent must not start a second fanout.
 *
 * Runs in every role except web (web only externalizes), matching the other in-process
 * module listeners.
 */
@Component
@ConditionalOnExpression("'${app.role:standalone}' != 'web'")
@RequiredArgsConstructor
class PostEventListener {

    private final JobScheduler jobs;

    @ApplicationModuleListener
    void on(PostCreatedEvent event) {
        jobs.enqueueOnce("feed.fanout", "fanout:" + event.postId(),
                Map.of("postId", event.postId(), "authorId", event.authorId()));
    }
}
