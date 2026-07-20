package com.premtsd.linkedin.notification;

import com.premtsd.linkedin.user.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * THE WORKER. This is the modulith's answer to Appwrite-style workers and to the
 * microservices' Kafka consumers.
 *
 * {@code @ApplicationModuleListener} = @Async + @Transactional(REQUIRES_NEW)
 * + @TransactionalEventListener(AFTER_COMMIT). So this method:
 *   - runs on a separate thread (async, off the request path),
 *   - only after the publishing transaction commits,
 *   - and is backed by the durable event registry, so if this handler fails or
 *     the app restarts mid-processing, the event is re-delivered (at-least-once,
 *     like Kafka) instead of being lost.
 *
 * Deploy-time: run the same jar with --spring.profiles.active=worker to host
 * only these listeners as a dedicated, independently-scaled worker process.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class UserEventWorker {

    private final NotificationService notificationService;

    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent event) {
        log.info("[worker] Welcoming new user id={}", event.userId());
        notificationService.create(event.userId(), "Welcome to LinkedIn, " + event.name() + "!");
    }
}
