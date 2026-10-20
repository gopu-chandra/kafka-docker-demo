package com.example.kafkademo;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Topics are declared in code, so they are created on startup with the
     * settings we choose (instead of relying on auto-create defaults).
     */
    @Bean
    NewTopic ordersTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic ordersDeadLetterTopic() {
        // Same partition count as the main topic, so a failed message keeps its partition.
        return TopicBuilder.name(KafkaTopics.ORDERS_DLT).partitions(3).replicas(1).build();
    }

    /**
     * What happens when the consumer throws an exception:
     *   1. Retry the same message 2 more times, 1 second apart.
     *   2. If it still fails, publish it to orders.DLT and move on,
     *      so one bad message doesn't block the whole partition.
     *
     * Spring Boot automatically plugs this bean into every @KafkaListener.
     */
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(KafkaTopics.ORDERS_DLT, record.partition()));

        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}
