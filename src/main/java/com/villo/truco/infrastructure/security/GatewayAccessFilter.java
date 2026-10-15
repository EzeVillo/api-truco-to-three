package com.villo.truco.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Impide saltarse el rate limiter externo llamando directamente al origen de Render. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@EnableConfigurationProperties(GatewayProperties.class)
public final class GatewayAccessFilter extends OncePerRequestFilter {

  private static final String SECRET_HEADER = "X-Truco-Gateway-Secret";
  private static final Set<String> PROBES = Set.of("/actuator/health/liveness",
      "/actuator/health/readiness");

  private final GatewayProperties properties;
  private final byte[] expectedSecret;

  public GatewayAccessFilter(final GatewayProperties properties) {
    this.properties = properties;
    this.expectedSecret = properties.secret() == null ? new byte[0]
        : properties.secret().getBytes(StandardCharsets.UTF_8);
    if (properties.enabled() && this.expectedSecret.length < 32) {
      throw new IllegalStateException("TRUCO_GATEWAY_SECRET must contain at least 32 bytes");
    }
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
      final HttpServletResponse response, final FilterChain filterChain)
      throws ServletException, IOException {
    final boolean probe = ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
        && PROBES.contains(request.getRequestURI());
    final var supplied = request.getHeader(SECRET_HEADER);
    if (!this.properties.enabled() || probe || (supplied != null
        && MessageDigest.isEqual(this.expectedSecret, supplied.getBytes(StandardCharsets.UTF_8)))) {
      filterChain.doFilter(request, response);
      return;
    }
    final var requestId = UUID.randomUUID().toString();
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Request-Id", requestId);
    response.getWriter().write("""
        {"errorCode":"GatewayAccessDeniedException","message":"Forbidden","timestamp":"%s","requestId":"%s"}
        """.formatted(Instant.now(), requestId));
  }
}
