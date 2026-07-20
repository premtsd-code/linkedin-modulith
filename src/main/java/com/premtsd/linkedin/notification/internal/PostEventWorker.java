package com.premtsd.linkedin.notification.internal;

import com.premtsd.linkedin.post.events.PostCreatedEvent;
import com.premtsd.linkedin.post.events.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Mirrors the microservices notification-service PostsServiceConsumer. */
@Component
@org.springframework.context.annotation.Profile("!web & !worker") // in-process only; Kafka split uses NotificationKafkaWorker
@RequiredArgsConstructor
@Slf4j
class PostEventWorker {

    private final NotificationService notificationService;

    @ApplicationModuleListener
    void onPostCreated(PostCreatedEvent event) {
        notificationService.create(event.authorId(), "Your post is live.");
    }

    @ApplicationModuleListener
    void onPostLiked(PostLikedEvent event) {
        notificationService.create(event.authorId(),
                "User " + event.likerId() + " liked your post.");
    }
}
