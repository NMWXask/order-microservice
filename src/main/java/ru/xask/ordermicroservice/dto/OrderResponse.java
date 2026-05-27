package ru.xask.ordermicroservice.dto;

import lombok.Builder;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;

@Builder
public record OrderResponse(
        Order orderDto,
        OrderItem itemDto
) {
}
