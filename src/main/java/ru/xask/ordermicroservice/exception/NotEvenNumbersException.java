package ru.xask.ordermicroservice.exception;

public class NotEvenNumbersException extends RuntimeException {
    public NotEvenNumbersException(String message) {
        super(message);
    }
}
