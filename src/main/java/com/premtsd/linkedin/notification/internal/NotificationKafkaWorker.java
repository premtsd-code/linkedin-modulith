package com.premtsd.linkedin.notification.internal;

import com.premtsd.linkedin.shared.KafkaTopics;
import com.premtsd.linkedin.connections.events.ConnectionAcceptedEvent;
import com.premtsd.linkedin.connections.events.ConnectionRequestedEvent;
import com.premtsd.linkedin.post.events.PostCreatedEvent;
import com.premtsd.linkedin.post.events.PostLikedEvent;
import com.premtsd.linkedin.user.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * WORKER TIER. The Kafka-driven counterpart of the in-process notification workers.
 * All instances share the 'notification-workers' group, so Kafka spreads each
 * topic's partitions across however many worker replicas are running -> scaling
 * workers up/down changes throughput with no code change.
 */
@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
class NotificationKafkaWorker {

    private static final String GROUP = "notification-workers";
    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = GROUP)
    void onUserRegistered(UserRegisteredEvent e) {
        notificationService.create(e.userId(), "Welcome to LinkedIn, " + e.name() + "!");
    }

    @KafkaListener(topics = KafkaTopics.POST_CREATED, groupId = GROUP)
    void onPostCreated(PostCreatedEvent e) {
        notificationService.create(e.authorId(), "Your post is live.");
    }

    @KafkaListener(topics = KafkaTopics.POST_LIKED, groupId = GROUP)
    void onPostLiked(PostLikedEvent e) {
        notificationService.create(e.authorId(), "User " + e.likerId() + " liked your post.");
    }

    @KafkaListener(topics = KafkaTopics.CONNECTION_REQUESTED, groupId = GROUP)
    void onConnectionRequested(ConnectionRequestedEvent e) {
        notificationService.create(e.receiverId(),
                "You received a connection request from user " + e.senderId() + ".");
    }

    @KafkaListener(topics = KafkaTopics.CONNECTION_ACCEPTED, groupId = GROUP)
    void onConnectionAccepted(ConnectionAcceptedEvent e) {
        notificationService.create(e.senderId(),
                "User " + e.receiverId() + " accepted your connection request.");
    }
}
