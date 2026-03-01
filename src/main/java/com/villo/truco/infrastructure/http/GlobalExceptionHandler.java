package com.villo.truco.infrastructure.http;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.domain.shared.DomainException;
import com.villo.truco.infrastructure.http.dto.response.ErrorResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(final ApplicationException ex) {

        return ResponseEntity.status(this.resolveStatus(ex.getStatus()))
            .body(new ErrorResponse(ex.getClass().getName(), ex.getMessage(), Instant.now()));
    }

    private HttpStatus resolveStatus(final ApplicationStatus status) {

        return switch (status) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(final DomainException ex) {

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(new ErrorResponse(ex.getClass().getName(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(final Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred", Instant.now()));
    }

}
