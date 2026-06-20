package ru.xask.ordermicroservice.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderStatusListener {

    @KafkaListener(topics = "order_events", groupId = "order_service")
    public void onEvent(String message) {
        log.info("Received event: {}", message);
    }
}
