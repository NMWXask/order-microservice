package ru.xask.ordermicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.xask.ordermicroservice.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}