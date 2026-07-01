package ru.xask.ordermicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.xask.ordermicroservice.entity.OrderItem;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT DISTINCT oi.order.id FROM OrderItem oi WHERE oi.category = :category")
    List<Long> findDistinctOrderIdsByCategory(String category);
}