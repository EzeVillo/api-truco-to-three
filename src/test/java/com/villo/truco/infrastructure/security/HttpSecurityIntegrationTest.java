package com.villo.truco.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.model.match.valueobjects.MatchId;
import com.villo.truco.domain.model.match.valueobjects.PlayerId;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(properties = {"truco.security.jwt-secret=test-secret-test-secret-test-secret",
    "truco.security.issuer=test-issuer",
    "truco.security.audience=test-audience"}, webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpSecurityIntegrationTest {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

  @Autowired
  private PlayerTokenProvider tokenProvider;

  @Test
  void shouldRejectMissingTokenOnProtectedEndpoint() throws Exception {

    final var matchId = UUID.randomUUID().toString();
    final var request = HttpRequest.newBuilder(
        URI.create(this.baseUrl() + "/api/matches/" + matchId)).GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
  }

  @Test
  void shouldRejectMalformedTokenOnProtectedEndpoint() throws Exception {

    final var matchId = UUID.randomUUID().toString();
    final var request = HttpRequest.newBuilder(
            URI.create(this.baseUrl() + "/api/matches/" + matchId))
        .header("Authorization", "Bearer malformed").GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
  }

  @Test
  void shouldRejectTokenFromAnotherMatch() throws Exception {

    final var tokenMatchId = new MatchId(UUID.randomUUID());
    final var requestedMatchId = UUID.randomUUID().toString();
    final var playerId = PlayerId.generate();

    final var token = this.tokenProvider.generateAccessToken(tokenMatchId, playerId);
    final var request = HttpRequest.newBuilder(
            URI.create(this.baseUrl() + "/api/matches/" + requestedMatchId))
        .header("Authorization", "Bearer " + token).GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
  }

  @Test
  void shouldAllowUnauthenticatedCreateMatchEndpoint() throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/matches"))
        .POST(HttpRequest.BodyPublishers.noBody()).build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
  }

  private String baseUrl() {

    return "http://localhost:" + this.port;
  }

}
