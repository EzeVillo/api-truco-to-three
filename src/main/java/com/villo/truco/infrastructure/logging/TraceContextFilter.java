package com.villo.truco.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public final class TraceContextFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TraceContextFilter.class);
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String REQUEST_ID_MDC_KEY = "requestId";
  private static final int MAX_PAYLOAD_LENGTH = 4096;

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
      final HttpServletResponse response, final FilterChain filterChain)
      throws ServletException, IOException {

    final var wrappedRequest = new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
    final var wrappedResponse = new ContentCachingResponseWrapper(response);
    final var requestId = this.resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
    final var startedAtMillis = System.currentTimeMillis();

    MDC.put(REQUEST_ID_MDC_KEY, requestId);
    wrappedResponse.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse);
      LOGGER.info("HTTP {} {} -> {} ({} ms) requestBody={} responseBody={}", request.getMethod(),
          request.getRequestURI(), wrappedResponse.getStatus(),
          System.currentTimeMillis() - startedAtMillis, this.readRequestPayload(wrappedRequest),
          this.readResponsePayload(wrappedResponse));
    } catch (final ServletException | IOException | RuntimeException ex) {
      LOGGER.error("HTTP {} {} failed after {} ms requestBody={} responseBody={}",
          request.getMethod(), request.getRequestURI(),
          System.currentTimeMillis() - startedAtMillis, this.readRequestPayload(wrappedRequest),
          this.readResponsePayload(wrappedResponse), ex);
      throw ex;
    } finally {
      wrappedResponse.copyBodyToResponse();
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }

  private String readRequestPayload(final ContentCachingRequestWrapper request) {

    if (!this.isLoggableContentType(request.getContentType())) {
      return "<not-loggable-content-type>";
    }

    final var payload = request.getContentAsByteArray();
    if (payload.length == 0) {
      return "<empty>";
    }

    return this.toSafeText(payload, request.getCharacterEncoding());
  }

  private String readResponsePayload(final ContentCachingResponseWrapper response) {

    if (!this.isLoggableContentType(response.getContentType())) {
      return "<not-loggable-content-type>";
    }

    final var payload = response.getContentAsByteArray();
    if (payload.length == 0) {
      return "<empty>";
    }

    return this.toSafeText(payload, response.getCharacterEncoding());
  }

  private boolean isLoggableContentType(final String contentType) {

    if (contentType == null || contentType.isBlank()) {
      return true;
    }

    return contentType.startsWith("application/json") || contentType.startsWith("application/xml")
        || contentType.startsWith("text/") || contentType.startsWith(
        "application/x-www-form-urlencoded");
  }

  private String toSafeText(final byte[] payload, final String encoding) {

    final var charset =
        encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding);

    final var maxLength = Math.min(payload.length, MAX_PAYLOAD_LENGTH);
    final var text = new String(payload, 0, maxLength, charset).replace("\n", "\\n")
        .replace("\r", "\\r");

    if (payload.length > MAX_PAYLOAD_LENGTH) {
      return text + "...<truncated>";
    }

    return text;
  }

  private String resolveRequestId(final String headerValue) {

    if (headerValue == null || headerValue.isBlank()) {
      return UUID.randomUUID().toString();
    }

    return headerValue.trim();
  }

}