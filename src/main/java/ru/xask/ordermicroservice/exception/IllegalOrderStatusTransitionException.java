package ru.xask.ordermicroservice.exception;

public class IllegalOrderStatusTransitionException extends RuntimeException {
    public IllegalOrderStatusTransitionException(String message) {
        super(message);
    }
}
