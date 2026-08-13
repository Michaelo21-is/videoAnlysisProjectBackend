package com.moj.purchaseservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        log.warn(
                "Invalid purchase request: {}",
                exception.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "code", "INVALID_REQUEST",
                        "message", "Invalid purchase request"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "Unhandled exception in purchase-service",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "SERVER_ERROR",
                        "message", "An unexpected error occurred"
                ));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of(
                        "message",
                        exception.getReason() != null
                                ? exception.getReason()
                                : "Request failed"
                ));
    }
}