package ru.xask.ordermicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendOrderEvent(String eventType, Long orderId) {
        String message = eventType + ":" + orderId;
        kafkaTemplate.send("order_events", message);
    }
}
