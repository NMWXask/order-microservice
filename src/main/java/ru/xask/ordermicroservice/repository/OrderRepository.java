package ru.xask.ordermicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.xask.ordermicroservice.entity.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // ОШИБКА: в запросе используется o.orderStatus, но в сущности поле называется status
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status")
    List<Order> findByStatus(String status);
}
