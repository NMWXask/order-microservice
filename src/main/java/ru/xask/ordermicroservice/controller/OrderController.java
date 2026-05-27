package ru.xask.ordermicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.xask.ordermicroservice.dto.CreateOrderRequest;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public OrderResponse createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        return orderService.createOrder(createOrderRequest);
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
}
