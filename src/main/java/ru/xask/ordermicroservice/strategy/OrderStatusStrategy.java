package ru.xask.ordermicroservice.strategy;

public interface OrderStatusStrategy {
    void validateTransition(String current, String newStatus);
}
