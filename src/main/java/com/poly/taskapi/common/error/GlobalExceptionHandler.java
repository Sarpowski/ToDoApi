package com.poly.taskapi.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
    return toResponse(ex.getStatus(), ex.getMessage(), request.getRequestURI(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpServletRequest request) {
    Map<String, Object> details = new LinkedHashMap<>();
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      fields.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    details.put("fields", fields);
    return toResponse(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
      HttpServletRequest request) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("violations", ex.getConstraintViolations().stream().map(v -> Map.of(
        "path", String.valueOf(v.getPropertyPath()),
        "message", v.getMessage()
    )).toList());
    return toResponse(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
      HttpServletRequest request) {
    return toResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", request.getRequestURI(), null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    return toResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI(), null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    return toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request.getRequestURI(), null);
  }

  private static ResponseEntity<ApiError> toResponse(HttpStatus status, String message, String path,
      Map<String, Object> details) {
    ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    return ResponseEntity.status(status).body(body);
  }
}
