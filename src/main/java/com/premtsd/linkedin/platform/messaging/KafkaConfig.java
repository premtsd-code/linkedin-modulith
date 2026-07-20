package com.premtsd.linkedin.platform.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic provisioning for the web↔worker split. Replaces the old
 * {@code shared/KafkaTopics}: the topic <em>names</em> now live on the events
 * themselves (via {@code @Externalized}); only the provisioning of the topics
 * lives here, and only under the web/worker profiles. 6 partitions per topic so a
 * consumer group can spread load across up to 6 worker replicas.
 */
@Configuration
@Profile("web | worker")
class KafkaConfig {

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }

    @Bean NewTopic userRegisteredTopic() { return topic("user-registered"); }
    @Bean NewTopic postCreatedTopic() { return topic("post-created"); }
    @Bean NewTopic postLikedTopic() { return topic("post-liked"); }
    @Bean NewTopic connectionRequestedTopic() { return topic("connection-requested"); }
    @Bean NewTopic connectionAcceptedTopic() { return topic("connection-accepted"); }
}
