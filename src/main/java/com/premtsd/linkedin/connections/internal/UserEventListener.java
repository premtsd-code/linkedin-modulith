package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.user.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the microservices connections-service: on a new user, create the
 * corresponding person in the connections graph. In-process event, not Kafka.
 */
@Component
@org.springframework.context.annotation.Profile("!web & !worker") // in-process only; Kafka split uses ConnectionKafkaWorker
@RequiredArgsConstructor
@Slf4j
class UserEventListener {

    private final ConnectionsService connectionsService;

    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent event) {
        log.info("[connections] registering person for user {}", event.userId());
        connectionsService.registerPerson(event.userId(), event.name());
    }
}
