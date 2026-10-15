package com.villo.truco.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@SpringBootTest(properties = {"spring.profiles.active=test", "truco.gateway.enabled=true",
    "truco.web.allowed-origins=https://ezevillo.com"},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIngressIntegrationTest {

  private static final String SECRET = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));
  private final HttpClient http = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

  @Autowired
  private JwtEncoder jwtEncoder;

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

  @Test
  @DisplayName("El ingreso protegido completa CONNECT y CONNECTED por WebSocket")
  void exchangesStompFramesThroughProtectedIngress() throws Exception {
    final var claims = JwtClaimsSet.builder().subject("11111111-1111-1111-1111-111111111111")
        .issuer("truco-api").audience(List.of("truco-clients"))
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    final var token = this.jwtEncoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    final var received = new CompletableFuture<String>();
    final var socket = this.http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5))
        .header("Origin", "https://ezevillo.com").header("X-Truco-Gateway-Secret", SECRET)
        .subprotocols("v12.stomp")
        .buildAsync(URI.create("ws://localhost:" + this.port + "/ws"), new WebSocket.Listener() {
          private final StringBuilder text = new StringBuilder();

          @Override
          public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data,
              final boolean last) {
            this.text.append(data);
            if (last) {
              received.complete(this.text.toString());
            }
            webSocket.request(1);
            return null;
          }

          @Override
          public void onError(final WebSocket webSocket, final Throwable error) {
            received.completeExceptionally(error);
          }
        }).get(5, TimeUnit.SECONDS);
    try {
      socket.sendText("CONNECT\naccept-version:1.2\nAuthorization:Bearer " + token
          + "\nheart-beat:0,0\n\n\0", true).get(5, TimeUnit.SECONDS);
      assertEquals("CONNECTED", received.get(5, TimeUnit.SECONDS).split("\n")[0]);
    } finally {
      socket.abort();
    }
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
