package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.shared.KafkaTopics;
import com.premtsd.linkedin.user.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * WORKER TIER. Kafka-driven counterpart of the in-process person-registration
 * listener. Its own consumer group, so it receives user events independently of
 * the notification workers.
 */
@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
class ConnectionKafkaWorker {

    private final ConnectionsService connectionsService;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "connection-workers")
    void onUserRegistered(UserRegisteredEvent e) {
        connectionsService.registerPerson(e.userId(), e.name());
    }
}
