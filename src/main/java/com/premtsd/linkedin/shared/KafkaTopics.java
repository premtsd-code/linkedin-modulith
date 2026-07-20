package com.premtsd.linkedin.shared;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topics carrying domain events from the web tier to the worker tier.
 * In the {@code shared} module so both the web bridge and the module workers can
 * reference the topic names without creating cross-module cycles. Beans are
 * created only under the 'web'/'worker' profiles; 6 partitions per topic so a
 * consumer group can spread load across up to 6 worker replicas.
 */
@Configuration
@Profile("web | worker")
public class KafkaTopics {

    public static final String USER_REGISTERED = "user-registered";
    public static final String POST_CREATED = "post-created";
    public static final String POST_LIKED = "post-liked";
    public static final String CONNECTION_REQUESTED = "connection-requested";
    public static final String CONNECTION_ACCEPTED = "connection-accepted";

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }

    @Bean NewTopic userRegisteredTopic() { return topic(USER_REGISTERED); }
    @Bean NewTopic postCreatedTopic() { return topic(POST_CREATED); }
    @Bean NewTopic postLikedTopic() { return topic(POST_LIKED); }
    @Bean NewTopic connectionRequestedTopic() { return topic(CONNECTION_REQUESTED); }
    @Bean NewTopic connectionAcceptedTopic() { return topic(CONNECTION_ACCEPTED); }
}
