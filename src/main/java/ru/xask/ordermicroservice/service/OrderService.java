package ru.xask.ordermicroservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;
import ru.xask.ordermicroservice.exception.OrderNotFoundException;
import ru.xask.ordermicroservice.mapper.DtoMapper;
import ru.xask.ordermicroservice.repository.OrderItemRepository;
import ru.xask.ordermicroservice.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducer;
    private final DtoMapper dtoMapper;

    @Transactional
    public OrderResponse createOrder(OrderDto orderDto) {
        Order order = dtoMapper.toEntity(orderDto);
        Order savedOrder = orderRepository.save(order);
        if (savedOrder.getItems() != null) {
            for (OrderItem item : savedOrder.getItems()) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }
        }
        log.debug("Order created successfully: {}", savedOrder.getId());
        kafkaProducer.sendOrderEvent("OrderCreated", savedOrder.getId());
        return dtoMapper.toResponse(savedOrder);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с id не найден: " + id));
        return dtoMapper.toResponse(order);
    }

    public List<OrderResponse> getAllOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
    }
}
