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
import ru.xask.ordermicroservice.strategy.OrderStatusStrategyImpl;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducer;
    private final OrderStatusStrategyImpl orderStatusStrategyImpl;
    private final DtoMapper dtoMapper;

    public OrderResponse createOrder(OrderDto orderDto) {
        Order order = dtoMapper.toEntity(orderDto);
        Order savedOrder = orderRepository.save(order);
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }
        }
        kafkaProducer.sendOrderEvent("OrderCreated", savedOrder.getId());
        return dtoMapper.toResponse(savedOrder);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        return dtoMapper.toResponse(order);
    }

    public double calculateTotalPrice(Order order) {
        return order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    public List<OrderResponse> getAllOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(dtoMapper::toResponse)
                .toList();
    }

    /**
     * Реализовать метод по обновлению статуса
     * Заказ имеет строго определённые переходы статусов:
     * NEW -> PROCESSING -> SHIPPED -> DELIVERED
     * CANCELLED может быть вызван только из статуса NEW или PROCESSING.
     * Если переход недопустим – выбросить кастомное исключение IllegalOrderStatusTransitionException с понятным сообщением.
     * При успешном обновлении отправить событие "OrderStatusChanged" с ID заказа через KafkaProducerService (предварительно исправив имя топика на единое).
     */
    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден: " + orderId));

        orderStatusStrategyImpl.validateTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus.toUpperCase());
        orderRepository.save(order);
        kafkaProducer.sendOrderEvent("OrderStatusChanged", orderId);
    }

    /**
     * Реализовать метод по фильтрации заказов по категории товара и минимальной сумме
     * вернуть заказы, которые содержат хотя бы один товар с заданной категорией.
     * Сумма всего заказа (цена * количество всех позиций) не меньше minTotal.
     */
    public List<OrderResponse> findOrdersByCategoryAndMinTotal(String category, double minTotal) {
        List<Long> orderIds = orderItemRepository.findDistinctOrderIdsByCategory(category);

        if (orderIds == null || orderIds.isEmpty()) {
            log.info("No orders found for category: {}", category);
            return List.of();
        }

        List<Order> orders = orderRepository.findOrdersByItemIds(orderIds);

        if (orders == null || orders.isEmpty()) {
            log.info("No orders loaded for ids: {}", orderIds);
            return List.of();
        }

        BigDecimal minTotalAmount = BigDecimal.valueOf(minTotal);

        return orders.stream()
                .filter(order -> {
                    BigDecimal total = order.getItems().stream()
                            .map(item -> BigDecimal.valueOf(item.getPrice())
                                    .multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return total.compareTo(minTotalAmount) >= 0;
                })
                .map(dtoMapper::toResponse)
                .toList();
    }

    /**
     * Расчёт скидки со сложными условиями (чтение и анализ чужого кода)
     * разобраться, что он делает, найти ошибки и отрефакторить
     */
    public double calculateDiscount(Order order) {
        double total = order.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
        if (order.getStatus().equals("CANCELLED")) {
            return 0;
        }
        if (total > 1000) {
            if (order.getCustomerName().startsWith("VIP")) {
                if (order.getItems().stream().anyMatch(i -> i.getCategory().equals("electronics"))) {
                    return total * 0.15;
                } else {
                    return total * 0.1;
                }
            } else {
                return total * 0.05;
            }
        } else if (order.getItems().size() > 5) {
            if (order.getCustomerName().contains("LOYAL")) {
                return total * 0.08;
            }
        }
        return 0;
    }

}