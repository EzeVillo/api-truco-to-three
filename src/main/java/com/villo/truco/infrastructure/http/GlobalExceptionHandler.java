package com.villo.truco.infrastructure.http;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponse> handleApplicationException(final ApplicationException ex) {

    final var status = this.resolveStatus(ex.getStatus());
    LOGGER.warn("Application exception mapped to {}: {}", status, ex.getMessage(), ex);

    return ResponseEntity.status(status)
        .body(new ErrorResponse(ex.getClass().getName(), ex.getMessage(), Instant.now()));
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

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ErrorResponse(ex.getClass().getName(), ex.getMessage(), Instant.now()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(final Exception ex) {

    LOGGER.error("Unexpected exception mapped to {}", HttpStatus.INTERNAL_SERVER_ERROR, ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred", Instant.now()));
  }

}
