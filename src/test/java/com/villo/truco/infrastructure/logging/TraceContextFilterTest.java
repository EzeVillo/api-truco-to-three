package com.villo.truco.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceContextFilterTest {

  @Test
  @DisplayName("Conserva la respuesta y la trazabilidad sin registrar credenciales ni tokens")
  void excludesSensitivePayloadsFromLogs() throws Exception {
    final var request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setContentType("application/json");
    request.setContent("{\"password\":\"private-password\"}".getBytes(StandardCharsets.UTF_8));
    request.addHeader("X-Request-Id", "gateway-request-123");
    final var response = new MockHttpServletResponse();
    final var logger = (Logger) LoggerFactory.getLogger(TraceContextFilter.class);
    final var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);

    try {
      new TraceContextFilter().doFilter(request, response, (req, res) -> {
        req.getInputStream().readAllBytes();
        res.getWriter().write("{\"accessToken\":\"private-token\"}");
      });

      assertEquals("{\"accessToken\":\"private-token\"}", response.getContentAsString());
      assertEquals("gateway-request-123", response.getHeader("X-Request-Id"));
      assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage()
          .contains("HTTP POST /api/auth/login -> 200")));
      assertFalse(appender.list.stream().anyMatch(event -> event.getFormattedMessage()
          .contains("private-password") || event.getFormattedMessage().contains("private-token")));
      assertNull(MDC.get("requestId"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  @DisplayName("Reemplaza identificadores de request inválidos")
  void replacesInvalidRequestId() throws Exception {
    final var request = new MockHttpServletRequest("GET", "/api/public/wake");
    final var invalidId = "untrusted request id";
    request.addHeader("X-Request-Id", invalidId);
    final var response = new MockHttpServletResponse();

    new TraceContextFilter().doFilter(request, response, (req, res) -> {});

    assertNotEquals(invalidId, response.getHeader("X-Request-Id"));
    assertTrue(response.getHeader("X-Request-Id").matches("[a-f0-9-]{36}"));
  }

  @Test
  @DisplayName("Limpia la trazabilidad cuando falla la cadena de filtros")
  void clearsContextAfterFailure() {
    final var request = new MockHttpServletRequest("POST", "/api/auth/login");
    final var response = new MockHttpServletResponse();

    assertThrows(ServletException.class, () -> new TraceContextFilter()
        .doFilter(request, response, (req, res) -> { throw new ServletException("test failure"); }));
    assertNull(MDC.get("requestId"));
  }
}
