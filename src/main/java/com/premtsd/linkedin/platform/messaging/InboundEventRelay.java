package com.premtsd.linkedin.platform.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * The inbound half of event externalization, and the only piece of Kafka plumbing
 * we own. Spring Modulith's {@code @Externalized} routes events OUT to Kafka on the
 * web tier; this relay reads them back IN on the worker tier and republishes them
 * as in-process application events, so the module workers ({@code NotificationWorker},
 * {@code PersonProjectionWorker}, feed listeners) process them through the exact same
 * {@code @ApplicationModuleListener} methods that run in standalone mode — one copy
 * of each rule, no drift.
 *
 * Generic by design: supporting a new event means adding its topic to the list, not
 * writing a method. Deserialization relies on the JsonDeserializer type headers plus
 * an explicit trusted-packages allow-list (see application.yml) — never wildcard it.
 *
 * Worker role only; the web tier has no relay bean.
 */
@Component
@ConditionalOnExpression("'${app.role:standalone}' == 'worker' and ${app.messaging.inbound-relay:true}")
@RequiredArgsConstructor
@Slf4j
class InboundEventRelay {

    private final ApplicationEventPublisher publisher;

    @KafkaListener(
            topics = {"user-registered", "post-created", "post-liked",
                    "connection-requested", "connection-accepted"},
            groupId = "${app.worker.group:linkedin-workers}")
    void relay(@Payload Object event) {
        log.debug("Relaying {} from Kafka to in-process listeners", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
}
