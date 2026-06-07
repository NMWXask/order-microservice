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

    /**
     * Реализовать метод по обновлению статуса
     * Заказ имеет строго определённые переходы статусов:
     * NEW -> PROCESSING -> SHIPPED -> DELIVERED
     * CANCELLED может быть вызван только из статуса NEW или PROCESSING.
     * Если переход недопустим – выбросить кастомное исключение IllegalOrderStatusTransitionException с понятным сообщением.
     * При успешном обновлении отправить событие "OrderStatusChanged" с ID заказа через KafkaProducerService (предварительно исправив имя топика на единое).
     */
    public void updateOrderStatus(Long orderId, String newStatus) {

    }

    /**
     Реализовать метод по фильтрации заказов по категории товара и минимальной сумме
     Вернуть заказы, которые содержат хотя бы один товар с заданной категорией.
     Сумма всего заказа (цена * количество всех позиций) не меньше minTotal.
     */
    public List<Order> findOrdersByCategoryAndMinTotal(String category, double minTotal) {

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
