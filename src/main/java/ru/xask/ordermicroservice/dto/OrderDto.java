package ru.xask.ordermicroservice.dto;

import lombok.Builder;

@Builder
public record OrderDto(
        Long id,
        String customerName,
        String status
) {
}
