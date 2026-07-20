package com.premtsd.linkedin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns domain errors into proper HTTP status codes for every module's controllers.
 * Without this, an {@code IllegalArgumentException} (e.g. duplicate signup) leaks as a 500.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> onBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
