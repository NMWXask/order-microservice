package ru.xask.ordermicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        Order order = orderService.getOrderById(id);
        return order;
    }

    @GetMapping("/by-status")
    public List<Order> getOrdersByStatus(@RequestParam String status) {
        List<Order> orders = orderService.getAllOrdersByStatus(status);
        orders.forEach(o -> System.out.println("Items count: " + o.getItems().size()));
        return orders;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable Long id, @RequestParam String newStatus) {
        orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok("Статус обновлён, событие отправлено");
    }
}
