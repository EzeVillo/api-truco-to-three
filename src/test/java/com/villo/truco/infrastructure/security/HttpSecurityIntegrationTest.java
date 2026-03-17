package com.villo.truco.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.villo.truco.application.ports.PlayerTokenProvider;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
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
  void shouldReturn404ForUnknownMatchWithValidToken() throws Exception {

    final var playerId = PlayerId.generate();
    final var requestedMatchId = UUID.randomUUID().toString();

    final var token = this.tokenProvider.generateAccessToken(playerId);
    final var request = HttpRequest.newBuilder(
            URI.create(this.baseUrl() + "/api/matches/" + requestedMatchId))
        .header("Authorization", "Bearer " + token).GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(404, response.statusCode());
  }

  @Test
  void shouldRequireAuthForCreateMatchEndpoint() throws Exception {

    final var body = "{\"gamesToPlay\":3}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/matches"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
  }

  @Test
  void shouldAllowUnauthenticatedRegisterEndpoint() throws Exception {

    final var body = "{\"username\":\"testuser\",\"password\":\"testpassword\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
  }

  @Test
  void shouldAllowUnauthenticatedLoginEndpoint() throws Exception {

    final var registerBody = "{\"username\":\"logintest\",\"password\":\"testpassword\"}";
    this.httpClient.send(HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerBody)).build(),
        HttpResponse.BodyHandlers.ofString());

    final var loginBody = "{\"username\":\"logintest\",\"password\":\"testpassword\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(loginBody)).build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
  }

  @Test
  void shouldAllowUnauthenticatedGuestEndpoint() throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/guest"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
  }

  @Test
  void shouldAllowPreflightForJoinEndpoint() throws Exception {

    final var matchId = UUID.randomUUID().toString();
    final var request = HttpRequest.newBuilder(
            URI.create(this.baseUrl() + "/api/matches/" + matchId + "/join"))
        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
        .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
  }

  private String baseUrl() {

    return "http://localhost:" + this.port;
  }

}
