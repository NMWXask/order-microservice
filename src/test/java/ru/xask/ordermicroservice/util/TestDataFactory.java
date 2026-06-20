package ru.xask.ordermicroservice.util;

import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderItemDto;

import java.util.List;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static OrderDto createDefaultOrderDto() {
        return OrderDto.builder()
                .customerName("Default Customer")
                .status("PENDING")
                .items(List.of(createDefaultOrderItemDto()))
                .build();
    }

    public static OrderItemDto createDefaultOrderItemDto() {
        return OrderItemDto.builder()
                .productName("Laptop")
                .category("Electronics")
                .price(1200.99)
                .quantity(1)
                .build();
    }
}