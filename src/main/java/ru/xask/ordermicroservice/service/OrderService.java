package ru.xask.ordermicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // импорт есть, но аннотация не используется
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;
import ru.xask.ordermicroservice.repository.OrderItemRepository;
import ru.xask.ordermicroservice.repository.OrderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducer;

    // ОШИБКА 1: нет @Transactional, при сбое после сохранения заказа данные будут неконсистентны
    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);   // заказ сохранён
        // Предположим, что здесь может быть ошибка, например, нехватка товара
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }
        }
        // Отправляем событие в Kafka
        kafkaProducer.sendOrderEvent("OrderCreated", savedOrder.getId());
        return savedOrder;
    }

    // ОШИБКА 2: метод кэшируется, но Order не Serializable – при первом вызове будет исключение
    @Cacheable(value = "orders", key = "#id")
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // ОШИБКА 3: логическая – забыта скидка на electronics
    public double calculateTotalPrice(Order order) {
        return order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())  // скидка не применяется
                .sum();
    }

    // ОШИБКА 4 (N+1): метод для получения всех заказов по статусу – в контроллере потом вызывается getItems().size()
    public List<Order> getAllOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);  // JPQL с ошибкой + N+1
    }
}
