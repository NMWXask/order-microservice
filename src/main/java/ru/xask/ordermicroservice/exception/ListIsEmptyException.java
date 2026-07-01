package ru.xask.ordermicroservice.exception;

public class ListIsEmptyException extends RuntimeException {
    public ListIsEmptyException(String message) {
        super(message);
    }
}
