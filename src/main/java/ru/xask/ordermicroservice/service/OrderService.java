package ru.xask.ordermicroservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.xask.ordermicroservice.dto.CreateOrderRequest;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;
import ru.xask.ordermicroservice.mapper.DtoMapper;
import ru.xask.ordermicroservice.repository.OrderItemRepository;
import ru.xask.ordermicroservice.repository.OrderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducer;
    private final DtoMapper dtoMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        Order order = dtoMapper.toEntity(createOrderRequest.orderDto());

        order = orderRepository.save(order);
        log.debug("Order created with ID: {}", order.getId());
        OrderItem orderItem = dtoMapper.toEntity(createOrderRequest.itemDto());
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
            }
            orderItemRepository.save(orderItem);
            log.debug("OrderItem created with ID: " + orderItem.getId());
        }
        kafkaProducer.sendOrderEvent("OrderCreated", order.getId());
        return new OrderResponse(order,orderItem);
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
        return orderRepository.findByStatus(status);//лист заказов
    }
}
