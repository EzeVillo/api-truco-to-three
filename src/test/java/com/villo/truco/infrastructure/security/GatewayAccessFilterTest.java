package com.villo.truco.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GatewayAccessFilterTest {

  private final String secret = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));

  @Test
  @DisplayName("Rechaza el origen directo sin filtrar la clave en la respuesta")
  void rejectsDirectAccess() throws Exception {
    final var request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.addHeader("X-Truco-Gateway-Secret", "incorrect");
    final var response = new MockHttpServletResponse();
    final var reached = new AtomicBoolean();
    new GatewayAccessFilter(new GatewayProperties(true, this.secret))
        .doFilter(request, response, (req, res) -> reached.set(true));
    assertFalse(reached.get());
    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("GatewayAccessDeniedException"));
    assertFalse(response.getContentAsString().contains(this.secret));
  }

  @Test
  @DisplayName("Permite el Worker autenticado y el desarrollo con protección desactivada")
  void permitsAuthorizedOrDisabledIngress() throws Exception {
    for (final var enabled : new boolean[] {true, false}) {
      final var request = new MockHttpServletRequest("GET", "/ws-sockjs/info");
      if (enabled) {
        request.addHeader("X-Truco-Gateway-Secret", this.secret);
      }
      final var reached = new AtomicBoolean();
      new GatewayAccessFilter(new GatewayProperties(enabled, this.secret))
          .doFilter(request, new MockHttpServletResponse(), (req, res) -> reached.set(true));
      assertTrue(reached.get());
    }
  }

  @Test
  @DisplayName("Sólo exceptúa GET y HEAD de los probes exactos")
  void exemptsOnlyExactHealthProbes() throws Exception {
    for (final var method : new String[] {"GET", "HEAD", "POST"}) {
      for (final var path : new String[] {"/actuator/health/liveness",
          "/actuator/health/readiness", "/actuator/health/liveness/other", "/api/public/wake"}) {
        final var reached = new AtomicBoolean();
        new GatewayAccessFilter(new GatewayProperties(true, this.secret))
            .doFilter(new MockHttpServletRequest(method, path), new MockHttpServletResponse(),
                (req, res) -> reached.set(true));
        assertEquals(!"POST".equals(method) && (path.endsWith("liveness")
            || path.endsWith("readiness")), reached.get());
      }
    }
  }

  @Test
  @DisplayName("Falla al activar sin clave fuerte y mantiene la clave fuera de toString")
  void validatesConfigurationWithoutExposingSecrets() {
    assertThrows(IllegalStateException.class,
        () -> new GatewayAccessFilter(new GatewayProperties(true, null)));
    assertThrows(IllegalStateException.class,
        () -> new GatewayAccessFilter(new GatewayProperties(true, "short")));
    assertFalse(new GatewayProperties(true, this.secret).toString().contains(this.secret));
  }
}
