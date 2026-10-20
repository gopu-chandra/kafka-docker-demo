package com.example.kafkademo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, Order> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends the order with orderId as the message KEY.
     * Kafka uses the key to pick a partition, so all events for the same order
     * always land on the same partition and are consumed in order.
     *
     * The send is asynchronous: we log the outcome when Kafka confirms it.
     */
    public CompletableFuture<SendResult<String, Order>> send(Order order) {
        return kafkaTemplate.send(KafkaTopics.ORDERS, order.orderId(), order)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        var meta = result.getRecordMetadata();
                        log.info("Sent order {} -> partition {}, offset {}",
                                order.orderId(), meta.partition(), meta.offset());
                    } else {
                        log.error("Failed to send order {}", order.orderId(), ex);
                    }
                });
    }
}
