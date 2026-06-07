package ru.xask.ordermicroservice.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderDto(
        Long id,
        String customerName,
        String status,
        List<OrderItemDto> items
) {
}
