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
