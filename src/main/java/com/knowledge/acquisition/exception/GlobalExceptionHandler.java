package com.knowledge.acquisition.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Map<String, Object>> notFound(NotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", System.currentTimeMillis()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    error ->
                        error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Invalid value",
                    (existing, replacement) -> existing + "; " + replacement));

    log.warn("Validation failed: {}", fieldErrors);

    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error",
                "Validation Failed",
                "message",
                "Request validation failed",
                "fieldErrors",
                fieldErrors,
                "timestamp",
                System.currentTimeMillis()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Map<String, Object>> constraintViolation(ConstraintViolationException ex) {
    Map<String, String> violations =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    ConstraintViolation::getMessage,
                    (existing, replacement) -> existing + "; " + replacement));

    log.warn("Constraint violation: {}", violations);

    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error",
                "Constraint Violation",
                "message",
                "Request constraint validation failed",
                "violations",
                violations,
                "timestamp",
                System.currentTimeMillis()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, Object>> illegalArgument(IllegalArgumentException ex) {
    log.warn("Invalid argument: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", System.currentTimeMillis()));
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<Map<String, Object>> illegalState(IllegalStateException ex) {
    log.error("Illegal state: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", System.currentTimeMillis()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred. Please contact support.",
                "timestamp", System.currentTimeMillis()));
  }
}
