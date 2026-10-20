package com.example.kafkademo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderProducer producer;

    public OrderController(OrderProducer producer) {
        this.producer = producer;
    }

    /** What the caller sends. The server assigns the orderId. */
    public record CreateOrderRequest(String product, int quantity, BigDecimal price) {
    }

    /**
     * Publishes an order to Kafka and returns immediately.
     * 202 Accepted means "received, will be processed", not "already processed",
     * because the consumer handles it asynchronously.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody CreateOrderRequest request) {
        var order = new Order(UUID.randomUUID().toString(),
                request.product(), request.quantity(), request.price()); 

        producer.send(order);

        return ResponseEntity.accepted().body(Map.of("orderId", order.orderId(), "status", "ACCEPTED"));
    }
}
