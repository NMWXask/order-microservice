package ru.xask.ordermicroservice.dto;

import lombok.Builder;

@Builder
public record ErrorResponse(
        String code,
        String message,
        String timeOfError
) {
}
