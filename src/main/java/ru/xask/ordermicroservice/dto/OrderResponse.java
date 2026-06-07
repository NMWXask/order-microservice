package ru.xask.ordermicroservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderResponse(
        Long id,
        String customerName,
        String status,
        List<OrderItemDto> items
) {
}
