package ru.xask.ordermicroservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
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


    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }
        }
        kafkaProducer.sendOrderEvent("OrderCreated", savedOrder.getId());
        return savedOrder;
    }

    @Cacheable(value = "orders", key = "#id")
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }


    public double calculateTotalPrice(Order order) {
        return order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }


    public List<Order> getAllOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }
}
