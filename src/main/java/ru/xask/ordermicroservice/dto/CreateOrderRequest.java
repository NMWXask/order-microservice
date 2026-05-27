package ru.xask.ordermicroservice.dto;

import lombok.Builder;
import ru.xask.ordermicroservice.entity.Order;
@Builder
public record CreateOrderRequest(
        Order orderDto,
        OrderItemDto itemDto
) {
}
