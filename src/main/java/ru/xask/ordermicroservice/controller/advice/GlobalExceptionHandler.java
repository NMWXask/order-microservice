package ru.xask.ordermicroservice.controller.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.xask.ordermicroservice.dto.ErrorResponse;
import ru.xask.ordermicroservice.exception.IllegalOrderStatusTransitionException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(IllegalOrderStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalOrderStatusTransitionException(IllegalOrderStatusTransitionException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INVALID_STATUS_TRANSITION")
                .message(ex.getMessage())
                .timeOfError(LocalDateTime.now().format(FORMATTER))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
}