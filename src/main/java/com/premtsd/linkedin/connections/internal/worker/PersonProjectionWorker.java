package com.premtsd.linkedin.connections.internal.worker;

import com.premtsd.linkedin.connections.internal.ConnectionsService;
import com.premtsd.linkedin.user.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Projects new users into the connections graph — the single replacement for the
 * old in-process {@code UserEventListener} and Kafka {@code ConnectionKafkaWorker}
 * pair. One rule, both delivery paths (see
 * {@link com.premtsd.linkedin.platform.messaging.InboundEventRelay}).
 *
 * Runs in every role except {@code web}, for the same reason as
 * {@code NotificationWorker}.
 */
@Component
@ConditionalOnExpression("'${app.role:standalone}' != 'web'")
@RequiredArgsConstructor
@Slf4j
class PersonProjectionWorker {

    private final ConnectionsService connectionsService;

    @ApplicationModuleListener
    void on(UserRegisteredEvent event) {
        log.info("[connections] registering person for user {}", event.userId());
        connectionsService.registerPerson(event.userId(), event.name());
    }
}
