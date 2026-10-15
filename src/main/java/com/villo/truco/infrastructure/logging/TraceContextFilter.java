package com.villo.truco.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class TraceContextFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TraceContextFilter.class);
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String REQUEST_ID_MDC_KEY = "requestId";

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
      final HttpServletResponse response, final FilterChain filterChain)
      throws ServletException, IOException {

    final var requestId = this.resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
    final var startedAtMillis = System.currentTimeMillis();

    MDC.put(REQUEST_ID_MDC_KEY, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      filterChain.doFilter(request, response);
      LOGGER.info("HTTP {} {} -> {} ({} ms)", request.getMethod(),
          request.getRequestURI(), response.getStatus(),
          System.currentTimeMillis() - startedAtMillis);
    } catch (final ServletException | IOException | RuntimeException ex) {
      LOGGER.error("HTTP {} {} failed after {} ms",
          request.getMethod(), request.getRequestURI(),
          System.currentTimeMillis() - startedAtMillis, ex);
      throw ex;
    } finally {
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }

  private String resolveRequestId(final String headerValue) {

    if (headerValue == null || !headerValue.matches("[A-Za-z0-9._:-]{1,128}")) {
      return UUID.randomUUID().toString();
    }

    return headerValue;
  }

}
