package ru.xask.ordermicroservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderItemDto;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.entity.Order;
import ru.xask.ordermicroservice.entity.OrderItem;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DtoMapper {

    Order toEntity(OrderDto orderDto);

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    OrderItem toEntity(OrderItemDto dto);

    OrderItemDto toResponse(OrderItem item);


}