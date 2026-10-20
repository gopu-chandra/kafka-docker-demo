package com.example.kafkademo;

import java.math.BigDecimal;

/**
 * The event we send through Kafka. It travels as JSON, for example:
 * {"orderId":"a1b2...","product":"Laptop","quantity":1,"price":999.99}
 */
public record Order(String orderId, String product, int quantity, BigDecimal price) {
}
