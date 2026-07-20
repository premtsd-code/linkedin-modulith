package com.premtsd.linkedin;

import com.premtsd.linkedin.shared.KafkaTopics;
import com.premtsd.linkedin.connections.events.ConnectionAcceptedEvent;
import com.premtsd.linkedin.connections.events.ConnectionRequestedEvent;
import com.premtsd.linkedin.post.events.PostCreatedEvent;
import com.premtsd.linkedin.post.events.PostLikedEvent;
import com.premtsd.linkedin.user.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * WEB TIER ONLY. Listens to in-process domain events (after commit) and forwards
 * them to Kafka, so the separately-deployed worker tier can consume and process
 * them. This is the bridge that lets workers scale independently of web.
 */
@Component
@Profile("web")
@RequiredArgsConstructor
class KafkaEventBridge {

    private final KafkaTemplate<String, Object> kafka;

    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent e) {
        kafka.send(KafkaTopics.USER_REGISTERED, String.valueOf(e.userId()), e);
    }

    @ApplicationModuleListener
    void onPostCreated(PostCreatedEvent e) {
        kafka.send(KafkaTopics.POST_CREATED, String.valueOf(e.authorId()), e);
    }

    @ApplicationModuleListener
    void onPostLiked(PostLikedEvent e) {
        kafka.send(KafkaTopics.POST_LIKED, String.valueOf(e.authorId()), e);
    }

    @ApplicationModuleListener
    void onConnectionRequested(ConnectionRequestedEvent e) {
        kafka.send(KafkaTopics.CONNECTION_REQUESTED, String.valueOf(e.receiverId()), e);
    }

    @ApplicationModuleListener
    void onConnectionAccepted(ConnectionAcceptedEvent e) {
        kafka.send(KafkaTopics.CONNECTION_ACCEPTED, String.valueOf(e.senderId()), e);
    }
}
