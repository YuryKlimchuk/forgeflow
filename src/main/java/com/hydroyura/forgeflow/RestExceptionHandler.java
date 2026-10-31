package com.hydroyura.forgeflow;

import com.hydroyura.forgeflow.catalog.application.DuplicatePartNumberException;
import com.hydroyura.forgeflow.catalog.application.PartNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Минимальный маппинг исключений каталога на HTTP-статусы.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(PartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(PartNotFoundException e) {
        return Map.of("error", "not_found", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(DuplicatePartNumberException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicate(DuplicatePartNumberException e) {
        return Map.of("error", "duplicate", "message", String.valueOf(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("error", "bad_request", "message", String.valueOf(e.getMessage()));
    }
}
