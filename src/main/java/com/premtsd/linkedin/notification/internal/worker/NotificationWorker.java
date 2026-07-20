package com.premtsd.linkedin.notification.internal.worker;

import com.premtsd.linkedin.connections.events.ConnectionAcceptedEvent;
import com.premtsd.linkedin.connections.events.ConnectionRequestedEvent;
import com.premtsd.linkedin.notification.internal.NotificationService;
import com.premtsd.linkedin.post.events.PostCreatedEvent;
import com.premtsd.linkedin.post.events.PostLikedEvent;
import com.premtsd.linkedin.user.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * The single notification worker — one copy of every rule, replacing the four
 * previous in-process/Kafka worker pairs. {@code @ApplicationModuleListener} makes
 * each method async, after-commit and backed by the durable event registry
 * (at-least-once, restart-safe).
 *
 * <p>Runs in every role except {@code web}: in standalone mode it consumes events
 * published in-process; in the worker role it consumes the same events after the
 * {@link com.premtsd.linkedin.platform.messaging.InboundEventRelay} republishes them
 * from Kafka. The web tier only externalizes, so it does not create the bean —
 * otherwise notifications would be produced twice.
 */
@Component
@ConditionalOnExpression("'${app.role:standalone}' != 'web'")
@RequiredArgsConstructor
@Slf4j
class NotificationWorker {

    private final NotificationService notifications;

    @ApplicationModuleListener
    void on(UserRegisteredEvent e) {
        log.info("[notification] welcoming new user id={}", e.userId());
        notifications.create(e.userId(), "Welcome to LinkedIn, " + e.name() + "!");
    }

    @ApplicationModuleListener
    void on(PostCreatedEvent e) {
        notifications.create(e.authorId(), "Your post is live.");
    }

    @ApplicationModuleListener
    void on(PostLikedEvent e) {
        notifications.create(e.authorId(), "User " + e.likerId() + " liked your post.");
    }

    @ApplicationModuleListener
    void on(ConnectionRequestedEvent e) {
        notifications.create(e.receiverId(),
                "You received a connection request from user " + e.senderId() + ".");
    }

    @ApplicationModuleListener
    void on(ConnectionAcceptedEvent e) {
        notifications.create(e.senderId(),
                "User " + e.receiverId() + " accepted your connection request.");
    }
}
