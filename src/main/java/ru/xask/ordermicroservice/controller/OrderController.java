package ru.xask.ordermicroservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public OrderResponse createOrder(@RequestBody OrderDto orderDto) {
        return orderService.createOrder(orderDto);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/by-status")
    public List<OrderResponse> getOrdersByStatus(@RequestParam String status) {
        return orderService.getAllOrdersByStatus(status);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable Long id, @RequestParam String newStatus) {
        orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok("Статус обновлён, событие отправлено");
    }

    @GetMapping("/filter")
    public List<OrderResponse> findOrdersByCategoryAndMinTotal(
            @RequestParam String category,
            @RequestParam double minTotal) {
        return orderService.findOrdersByCategoryAndMinTotal(category, minTotal);
    }
}