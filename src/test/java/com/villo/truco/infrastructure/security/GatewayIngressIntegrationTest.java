package com.villo.truco.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {"spring.profiles.active=test", "truco.gateway.enabled=true",
    "truco.web.allowed-origins=https://ezevillo.com"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIngressIntegrationTest {

  private static final String SECRET = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));
  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

  @DynamicPropertySource
  static void ingressProperties(final DynamicPropertyRegistry registry) {
    registry.add("truco.gateway.secret", () -> SECRET);
  }

  @Test
  @DisplayName("Render permite su probe y bloquea llamadas directas a REST y SockJS")
  void blocksBypassWithoutBreakingProbes() throws Exception {
    for (final var path : new String[] {"/api/public/wake", "/ws-sockjs/info"}) {
      assertEquals(403, this.send(path, false).statusCode());
      final var allowed = this.send(path, true);
      assertEquals(200, allowed.statusCode());
      assertEquals("https://ezevillo.com",
          allowed.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }
    assertEquals(200, this.send("/actuator/health/liveness", false).statusCode());
  }

  @Test
  @DisplayName("La clave del gateway no reemplaza el JWT del usuario")
  void stillRequiresUserAuthentication() throws Exception {
    assertEquals(401, this.send("/api/auth/me", true).statusCode());
  }

  private HttpResponse<String> send(final String path, final boolean throughGateway)
      throws Exception {
    final var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path))
        .header("Origin", "https://ezevillo.com");
    if (throughGateway) {
      builder.header("X-Truco-Gateway-Secret", SECRET);
    }
    return this.http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
  }
}
