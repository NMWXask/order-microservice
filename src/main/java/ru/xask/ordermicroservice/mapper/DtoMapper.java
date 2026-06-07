package ru.xask.ordermicroservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.xask.ordermicroservice.dto.OrderDto;
import ru.xask.ordermicroservice.dto.OrderResponse;
import ru.xask.ordermicroservice.entity.Order;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface DtoMapper {

    Order toEntity(OrderDto orderDto);

    OrderResponse toResponse(Order order);
}
