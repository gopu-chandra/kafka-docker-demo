package com.example.kafkademo;

/** Topic names in one place, so producer, consumer and config never disagree. */
public final class KafkaTopics {

    public static final String ORDERS = "orders";

    /** Dead-letter topic: messages that still fail after all retries end up here. */
    public static final String ORDERS_DLT = "orders.DLT";

    private KafkaTopics() {
    }
}
