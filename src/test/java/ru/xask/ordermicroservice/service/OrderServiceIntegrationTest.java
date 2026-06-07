package ru.xask.ordermicroservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.repository.OrderRepository;
import ru.xask.ordermicroservice.util.KafkaConsumerHelper;
import ru.xask.ordermicroservice.util.TestDataFactory;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class OrderServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldSendOrderCreatedEventToKafka() {
        OrderDto orderDto = TestDataFactory.createDefaultOrderDto();

        OrderResponse response = orderService.createOrder(orderDto);
        Long orderId = response.id();

        boolean messageFound = KafkaConsumerHelper.waitForMessage(
                kafka,
                "order_events",
                "OrderCreated:" + orderId,
                Duration.ofSeconds(5)
        );

        assertThat(messageFound)
                .withFailMessage("Не найдено сообщение OrderCreated:%d в топике order_events", orderId)
                .isTrue();
    }

    @Test
    @Transactional
    void shouldCreateOrderAndSendOrderCreatedEventToKafka() {

        OrderDto orderDto = TestDataFactory.createDefaultOrderDto();

        OrderResponse response = orderService.createOrder(orderDto);
        Long orderId = response.id();

        assertThat(response.id()).isNotNull();
        assertThat(response.customerName()).isEqualTo(orderDto.customerName());
        assertThat(response.status()).isEqualTo(orderDto.status());
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Laptop");

        Optional<Order> savedOrderOpt = orderRepository.findById(orderId);
        assertThat(savedOrderOpt).isPresent();
        Order savedOrder = savedOrderOpt.get();
        assertThat(savedOrder.getCustomerName()).isEqualTo(orderDto.customerName());
        assertThat(savedOrder.getStatus()).isEqualTo(orderDto.status());
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getItems().get(0).getProductName()).isEqualTo("Laptop");

        boolean messageFound = KafkaConsumerHelper.waitForMessage(
                kafka,
                "order_events",
                "OrderCreated:" + orderId,
                Duration.ofSeconds(5)
        );
        assertThat(messageFound)
                .withFailMessage("Сообщение OrderCreated:%d не найдено в топике order_events", orderId)
                .isTrue();
    }
}