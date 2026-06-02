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

/**
 * Global exception handler for REST API controllers.
 *
 * <p>This handler centralizes exception handling across all controllers, providing consistent error
 * responses with appropriate HTTP status codes and error details.
 *
 * <h3>Exception Handling Strategy</h3>
 *
 * <table>
 *   <tr>
 *     <th>Exception</th>
 *     <th>HTTP Status</th>
 *     <th>Use Case</th>
 *   </tr>
 *   <tr>
 *     <td>NotFoundException</td>
 *     <td>404 NOT FOUND</td>
 *     <td>Resource not found (project, process, etc.)</td>
 *   </tr>
 *   <tr>
 *     <td>MethodArgumentNotValidException</td>
 *     <td>400 BAD REQUEST</td>
 *     <td>Request body validation failure (@Valid)</td>
 *   </tr>
 *   <tr>
 *     <td>ConstraintViolationException</td>
 *     <td>400 BAD REQUEST</td>
 *     <td>Path/query parameter validation failure</td>
 *   </tr>
 *   <tr>
 *     <td>IllegalArgumentException</td>
 *     <td>400 BAD REQUEST</td>
 *     <td>Invalid input or business logic violation</td>
 *   </tr>
 *   <tr>
 *     <td>IllegalStateException</td>
 *     <td>409 CONFLICT</td>
 *     <td>Operation conflicts with current state</td>
 *   </tr>
 *   <tr>
 *     <td>Exception (catch-all)</td>
 *     <td>500 INTERNAL SERVER ERROR</td>
 *     <td>Unexpected errors</td>
 *   </tr>
 * </table>
 *
 * <h3>Error Response Format</h3>
 *
 * All error responses follow a consistent JSON structure:
 *
 * <pre>
 * {
 *   "error": "Error Type",
 *   "message": "Human-readable error message",
 *   "timestamp": 1234567890,
 *   "fieldErrors": { ... }  // Only for validation errors
 * }
 * </pre>
 *
 * <h3>Logging</h3>
 *
 * <ul>
 *   <li><b>WARN level:</b> Client errors (4xx) - not found, validation failures
 *   <li><b>ERROR level:</b> Server errors (5xx) - unexpected exceptions with stack traces
 * </ul>
 *
 * @see NotFoundException
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handles resource not found exceptions.
   *
   * @param ex the exception containing the resource identifier
   * @return 404 NOT FOUND with error details
   */
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

  /**
   * Handles request body validation failures.
   *
   * <p>Triggered when {@code @Valid} annotation finds constraint violations in request DTOs.
   * Returns field-level error details for each validation failure.
   *
   * @param ex the exception containing field validation errors
   * @return 400 BAD REQUEST with field-level error details
   */
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

  /**
   * Handles constraint violations on path and query parameters.
   *
   * <p>Triggered when method-level constraint annotations (e.g., {@code @Min}, {@code @NotNull}) on
   * controller method parameters are violated.
   *
   * @param ex the exception containing constraint violations
   * @return 400 BAD REQUEST with constraint violation details
   */
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

  /**
   * Handles illegal argument exceptions.
   *
   * <p>Triggered when business logic detects invalid input or parameter values (e.g., invalid
   * version number, unsupported file type, invalid configuration).
   *
   * @param ex the exception containing the error message
   * @return 400 BAD REQUEST with error message
   */
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

  /**
   * Handles illegal state exceptions.
   *
   * <p>Triggered when an operation is attempted that conflicts with the current system state (e.g.,
   * starting a process on a suspended project, duplicate version creation).
   *
   * @param ex the exception containing the error message
   * @return 409 CONFLICT with error message
   */
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

  /**
   * Handles all unhandled exceptions (catch-all).
   *
   * <p>This handler catches any exception not explicitly handled by other methods, ensuring the API
   * always returns a well-formed error response instead of a stack trace.
   *
   * <p>The exception is logged at ERROR level with full stack trace for investigation.
   *
   * @param ex the unhandled exception
   * @return 500 INTERNAL SERVER ERROR with generic error message (actual error details logged
   *     server-side)
   */
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
