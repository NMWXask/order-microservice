package ru.xask.ordermicroservice.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderStatusListener {

    // ОШИБКА: слушает топик "order-events", а продюсер шлёт в "order_events"
    @KafkaListener(topics = "order-events", groupId = "order-service")
    public void onEvent(String message) {
        log.info("Received event: {}", message);
    }
}
