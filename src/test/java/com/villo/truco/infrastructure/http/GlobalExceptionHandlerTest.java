package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.exceptions.ApplicationException;
import com.villo.truco.application.exceptions.ApplicationStatus;
import com.villo.truco.auth.domain.model.auth.exceptions.InvalidUserSessionRefreshException;
import com.villo.truco.domain.shared.DomainException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("MethodArgumentNotValidException -> 400 con errores de campo en message")
  void validationErrors() {

    final var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "username", "must not be blank"));
    bindingResult.addError(new FieldError("request", "password", "must not be blank"));

    final var ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(bindingResult);

    final var response = handler.handleValidationException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().message()).contains("username: must not be blank");
    assertThat(response.getBody().message()).contains("password: must not be blank");
  }

  @Test
  @DisplayName("HttpMessageNotReadableException sin causa especifica -> 400 con mensaje generico")
  void missingBody() {

    final var ex = new HttpMessageNotReadableException("Required request body is missing",
        new MockHttpInputMessage(new byte[0]));

    final var response = handler.handleMessageNotReadable(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
    assertThat(response.getBody().message()).isEqualTo("Malformed or missing request body");
  }

  @Test
  @DisplayName("ApplicationException BAD_REQUEST -> 400")
  void applicationBadRequest() {

    final var ex = new ApplicationException(ApplicationStatus.BAD_REQUEST, "invalid enum") {
    };

    final var response = handler.handleApplicationException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("invalid enum");
  }

  @Test
  @DisplayName("ApplicationException NOT_FOUND -> 404")
  void applicationNotFound() {

    final var ex = new ApplicationException(ApplicationStatus.NOT_FOUND, "not found") {
    };

    final var response = handler.handleApplicationException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("InvalidUserSessionRefreshException -> 401")
  void invalidUserSessionRefresh() {

    final var ex = new InvalidUserSessionRefreshException();

    final var response = handler.handleInvalidUserSessionRefresh(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("InvalidUserSessionRefreshException");
  }

  @Test
  @DisplayName("DomainException -> 422")
  void domainException() {

    final var ex = new DomainException("invalid state");

    final var response = handler.handleDomainException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  @DisplayName("Exception generica -> 500 sin exponer internals")
  void unexpectedException() {

    final var ex = new RuntimeException("db connection failed");

    final var response = handler.handleUnexpectedException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    assertThat(response.getBody().message()).doesNotContain("db connection failed");
  }

  @Test
  @DisplayName("NoResourceFoundException -> 404 con metodo y path en message")
  void resourceNotFound() {

    final var ex = mock(NoResourceFoundException.class);
    when(ex.getHttpMethod()).thenReturn(HttpMethod.GET);
    when(ex.getResourcePath()).thenReturn("/api/nonexistent");

    final var response = handler.handleNoResourceFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(response.getBody().message()).contains("GET");
    assertThat(response.getBody().message()).contains("/api/nonexistent");
  }

  @Test
  @DisplayName("HttpRequestMethodNotSupportedException -> 405 con metodos soportados")
  void methodNotAllowed() {

    final var ex = new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"));

    final var response = handler.handleMethodNotSupported(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("METHOD_NOT_ALLOWED");
    assertThat(response.getBody().message()).contains("DELETE");
    assertThat(response.getBody().message()).contains("Supported methods");
  }

}
