package ru.xask.ordermicroservice.dto;

import lombok.Builder;
import ru.xask.ordermicroservice.entity.Order;
@Builder
public record OrderItemDto(
        Long id,
        String productName,
        String category,
        double price,
        int quantity,
        Order order
) {
}
