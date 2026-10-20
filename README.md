# Kafka Demo: Order Producer & Consumer

A small Spring Boot 4 app that publishes orders to Kafka and consumes them.

```
POST /orders  ->  OrderProducer  ->  [orders topic]  ->  OrderConsumer
                                                            |
                                          fails 3 times? -> [orders.DLT] -> dead-letter logger
```

## What's in it

| File | Role |
|---|---|
| `Order.java` | The event, sent as JSON |
| `OrderController.java` | `POST /orders` endpoint that publishes an order |
| `OrderProducer.java` | Sends to Kafka with `orderId` as the key |
| `OrderConsumer.java` | `@KafkaListener` that processes orders, plus a dead-letter logger |
| `KafkaConfig.java` | Creates topics, sets up retries and the dead-letter topic |
| `application.yml` | Connection, JSON serialization, reliability settings |

## Run it

**1. Start Kafka (and the UI) in Docker**, from this folder:
```
docker compose up -d kafka kafka-ui
```

**2. Start the app from VS Code.** Open `KafkaDemoApplication.java` and click **Run** above `main`.
It runs on http://localhost:8081.

## Test it

**Send a good order:**
```
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product":"Laptop","quantity":2,"price":999.99}'
```
In the VS Code console you'll see `Sent order ...` then `Received order ...` and `Processed order ...`.

**Send a bad order** (quantity 0) to see retries and dead-lettering:
```
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product":"Phone","quantity":0,"price":499}'
```
You'll see the consumer try 3 times, about 1 second apart, then `DEAD LETTER: order ... failed permanently`.

**Send from the terminal instead of the app:**
```
kp --topic orders
{"orderId":"manual-1","product":"Mouse","quantity":1,"price":25}
```

**Watch it in the UI:** http://localhost:8080 shows the `orders` and `orders.DLT` topics, their messages, and the consumer groups.

## Run the app in Docker too (optional)

Stop the VS Code copy first, then:
```
docker compose up -d --build
```

## Industry practices used

- **Message keys:** same order always goes to the same partition, which preserves ordering per order.
- **JSON with typed records:** messages are readable in the UI and map straight to `Order`.
- **`acks=all` + idempotence:** messages aren't lost or duplicated when the producer retries.
- **Retries + dead-letter topic:** a bad message is retried, then set aside instead of blocking the partition.
- **ErrorHandlingDeserializer:** malformed JSON doesn't crash the consumer in an endless loop.
- **Topics declared in code:** partition count is explicit, not left to broker defaults.
