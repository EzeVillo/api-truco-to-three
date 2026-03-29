package com.villo.truco.infrastructure.http;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String REQUEST_ID_KEY = "requestId";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      final MethodArgumentNotValidException ex) {

    final var message = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).collect(Collectors.joining("; "));

    LOGGER.warn("Validation failed: {}", message);

    return ResponseEntity.badRequest().body(
        new ErrorResponse("VALIDATION_ERROR", message, Instant.now(), MDC.get(REQUEST_ID_KEY)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(
      final HttpMessageNotReadableException ex) {

    final String message;
    if (ex.getCause() instanceof MismatchedInputException mie && mie.getPath() != null
        && !mie.getPath().isEmpty()) {
      final var field = mie.getPath().stream().map(JsonMappingException.Reference::getFieldName)
          .filter(Objects::nonNull).collect(Collectors.joining("."));
      message = "Invalid value for field '" + field + "'";
    } else {
      message = "Malformed or missing request body";
    }

    LOGGER.warn("Message not readable: {}", message);

    return ResponseEntity.badRequest()
        .body(new ErrorResponse("BAD_REQUEST", message, Instant.now(), MDC.get(REQUEST_ID_KEY)));
  }

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponse> handleApplicationException(final ApplicationException ex) {

    final var status = this.resolveStatus(ex.getStatus());
    LOGGER.warn("Application exception mapped to {}: {}", status, ex.getMessage(), ex);

    return ResponseEntity.status(status).body(
        new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage(), Instant.now(),
            MDC.get(REQUEST_ID_KEY)));
  }

  private HttpStatus resolveStatus(final ApplicationStatus status) {

    return switch (status) {
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_CONTENT;
    };
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainException(final DomainException ex) {

    LOGGER.warn("Domain exception mapped to {}: {}", HttpStatus.UNPROCESSABLE_CONTENT,
        ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
        new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage(), Instant.now(),
            MDC.get(REQUEST_ID_KEY)));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(final NoResourceFoundException ex) {

    final var message = "No endpoint found for " + ex.getHttpMethod() + " " + ex.getResourcePath();
    LOGGER.warn("Resource not found: {}", message);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        new ErrorResponse("RESOURCE_NOT_FOUND", message, Instant.now(), MDC.get(REQUEST_ID_KEY)));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      final HttpRequestMethodNotSupportedException ex) {

    final var supported = ex.getSupportedHttpMethods();
    final var message = "Method " + ex.getMethod() + " is not supported for this endpoint" + (
        supported != null && !supported.isEmpty() ? ". Supported methods: " + supported : "");
    LOGGER.warn("Method not supported: {}", message);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
        new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.name(), message, Instant.now(),
            MDC.get(REQUEST_ID_KEY)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(final Exception ex) {

    LOGGER.error("Unexpected exception mapped to {}", HttpStatus.INTERNAL_SERVER_ERROR, ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), "An unexpected error occurred",
            Instant.now(), MDC.get(REQUEST_ID_KEY)));
  }

}
