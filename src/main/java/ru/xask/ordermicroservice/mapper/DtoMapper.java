package ru.xask.ordermicroservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.xask.ordermicroservice.dto.CreateOrderRequest;
import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderItemDto;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface DtoMapper {
    CreateOrderRequest toEntity(OrderDto orderDto, OrderItemDto itemDto);

    CreateOrderRequest toDto(OrderDto orderDto, OrderItemDto itemDto);

    Order toEntity(Order orderDto);

    OrderDto toDto(Order orderDto);

    OrderItem toEntity(OrderItemDto orderItemDto);

}
