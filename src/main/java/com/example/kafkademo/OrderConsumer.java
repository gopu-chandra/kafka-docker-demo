package com.example.kafkademo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    /**
     * Main consumer. Every instance of this app joins the "order-service" group,
     * so if you run two copies, Kafka splits the partitions between them.
     */
    @KafkaListener(topics = KafkaTopics.ORDERS, groupId = "order-service")
    public void consume(ConsumerRecord<String, Order> record) {
        Order order = record.value();
        log.info("Received order {} (key={}, partition={}, offset={}): {} x {} @ {}",
                order.orderId(), record.key(), record.partition(), record.offset(),
                order.quantity(), order.product(), order.price());

        // Simple business rule, so you can watch retries + dead-lettering happen.
        if (order.quantity() <= 0) {
            throw new IllegalStateException("Invalid quantity: " + order.quantity());
        }

        // Real work would go here: save to a database, call another service, etc.
        log.info("Processed order {}", order.orderId());
    }

    /**
     * Watches the dead-letter topic, so failed messages are visible in the logs.
     * In production you would alert on this, or store the messages for review.
     */
    @KafkaListener(topics = KafkaTopics.ORDERS_DLT, groupId = "order-dlt-monitor")
    public void consumeDeadLetter(ConsumerRecord<String, Order> record,
                                  @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false)
                                  String error) {
        log.warn("DEAD LETTER: order {} failed permanently. Reason: {}", record.key(), error);
    }
}
