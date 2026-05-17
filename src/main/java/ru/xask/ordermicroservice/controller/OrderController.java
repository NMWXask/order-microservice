package ru.xask.ordermicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        // Возвращаем сущность напрямую -> рекурсия
        Order order = orderService.getOrderById(id);
        // Ещё и вызов getItems() для демонстрации N+1 можно сделать здесь, но не обязательно
        return order;
    }

    @GetMapping("/by-status")
    public List<Order> getOrdersByStatus(@RequestParam String status) {
        List<Order> orders = orderService.getAllOrdersByStatus(status);
        // Для демонстрации N+1: обращаемся к ленивому списку для каждого заказа
        orders.forEach(o -> System.out.println("Items count: " + o.getItems().size()));
        return orders;
    }
}
