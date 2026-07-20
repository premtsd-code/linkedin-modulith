package com.premtsd.linkedin.notification.internal;

import com.premtsd.linkedin.connections.events.ConnectionAcceptedEvent;
import com.premtsd.linkedin.connections.events.ConnectionRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Mirrors the microservices notification-service ConnectionsServiceConsumer. */
@Component
@org.springframework.context.annotation.Profile("!web & !worker") // in-process only; Kafka split uses NotificationKafkaWorker
@RequiredArgsConstructor
@Slf4j
class ConnectionEventWorker {

    private final NotificationService notificationService;

    @ApplicationModuleListener
    void onRequestSent(ConnectionRequestedEvent event) {
        notificationService.create(event.receiverId(),
                "You received a connection request from user " + event.senderId() + ".");
    }

    @ApplicationModuleListener
    void onRequestAccepted(ConnectionAcceptedEvent event) {
        notificationService.create(event.senderId(),
                "User " + event.receiverId() + " accepted your connection request.");
    }
}
