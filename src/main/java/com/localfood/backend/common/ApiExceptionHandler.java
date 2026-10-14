package com.localfood.backend.common;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "message", ex.getReason() == null ? "Request denied" : ex.getReason()
        ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "timestamp", OffsetDateTime.now().toString(),
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> Map.of(
                "field", err.getField(),
                "message", err.getDefaultMessage() == null ? "Invalid value" : err.getDefaultMessage()
            ))
            .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", OffsetDateTime.now().toString());
        payload.put("message", "Validation failed");
        payload.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
    }
}
