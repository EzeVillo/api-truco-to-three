package com.villo.truco.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(properties = {"truco.security.jwt-secret=test-secret-test-secret-test-secret",
    "truco.security.issuer=test-issuer", "truco.security.audience=test-audience",
    "spring.profiles.active=test"}, webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpSecurityIntegrationTest {

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @LocalServerPort
  private int port;

  @Autowired
  private AccessTokenIssuer tokenIssuer;

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

    final var token = this.tokenIssuer.issueForUser(playerId).value();
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

    final var body = "{\"username\":\"testuser\",\"password\":\"test1!\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"refreshToken\""));
  }

  @Test
  void shouldAllowUnauthenticatedLoginEndpoint() throws Exception {

    final var registerBody = "{\"username\":\"logintest\",\"password\":\"test1!\"}";
    this.httpClient.send(HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerBody)).build(),
        HttpResponse.BodyHandlers.ofString());

    final var loginBody = "{\"username\":\"logintest\",\"password\":\"test1!\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(loginBody)).build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"refreshToken\""));
  }

  @Test
  void shouldRefreshAndRejectReplayOfOldRefreshToken() throws Exception {

    final var loginResponse = this.loginAndParse("refreshableuser");
    final var originalRefreshToken = (String) loginResponse.get("refreshToken");

    final var refreshResponse = this.httpClient.send(
        HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/refresh"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                "{\"refreshToken\":\"" + originalRefreshToken + "\"}")).build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(200, refreshResponse.statusCode());
    final var refreshed = this.readJson(refreshResponse.body());
    final var rotatedRefreshToken = (String) refreshed.get("refreshToken");

    final var replayOld = this.httpClient.send(
        HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/refresh"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                "{\"refreshToken\":\"" + originalRefreshToken + "\"}")).build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(401, replayOld.statusCode());

    final var rotatedAfterReplay = this.httpClient.send(
        HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/refresh"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                "{\"refreshToken\":\"" + rotatedRefreshToken + "\"}")).build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(401, rotatedAfterReplay.statusCode());
  }

  @Test
  void shouldAllowUnauthenticatedGuestEndpoint() throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/guest"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertFalse(response.body().contains("\"refreshToken\""));
    assertTrue(response.body().contains("\"accessTokenExpiresIn\""));
  }

  @Test
  void shouldAllowUnauthenticatedLogoutEndpoint() throws Exception {

    final var loginResponse = this.loginAndParse("logoutuser");
    final var refreshToken = (String) loginResponse.get("refreshToken");

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/logout"))
        .method("DELETE",
            HttpRequest.BodyPublishers.ofString("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .header("Content-Type", "application/json").build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(204, response.statusCode());
  }

  @Test
  void shouldRejectWeakRegisterCredentials() throws Exception {

    final var body = "{\"username\":\"ab\",\"password\":\"abcd\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("username: must contain at least 3 letters"));
    assertTrue(response.body().contains("password: must have at least 5 characters"));
  }

  @Test
  void shouldRejectRegisterUsernameWithSymbols() throws Exception {

    final var body = "{\"username\":\"abc!\",\"password\":\"test1!\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("username: must contain only letters and numbers"));
  }

  @Test
  void shouldRejectLoginUsernameWithSymbols() throws Exception {

    final var body = "{\"username\":\"abc!\",\"password\":\"test1!\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/login"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("username: must contain only letters and numbers"));
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

  @Test
  void shouldAllowMissingTokenOnActuatorHealth() throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/actuator/health"))
        .GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertTrue(response.statusCode() == 200 || response.statusCode() == 503);
  }

  @Test
  void shouldAllowActuatorReadinessWithoutToken() throws Exception {

    final var request = HttpRequest.newBuilder(
        URI.create(this.baseUrl() + "/actuator/health/readiness")).GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertTrue(response.statusCode() == 200 || response.statusCode() == 503);
  }

  @Test
  void shouldRejectMissingTokenOnActuatorMetrics() throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/actuator/metrics"))
        .GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
  }

  @Test
  void shouldReturn404WithErrorResponseForNonexistentEndpoint() throws Exception {

    final var playerId = PlayerId.generate();
    final var token = this.tokenIssuer.issueForUser(playerId).value();
    final var request = HttpRequest.newBuilder(
            URI.create(this.baseUrl() + "/api/this-does-not-exist"))
        .header("Authorization", "Bearer " + token).GET().build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(404, response.statusCode());
    assertTrue(response.body().contains("RESOURCE_NOT_FOUND"));
    assertTrue(response.body().contains("No endpoint found for"));
  }

  @Test
  void shouldReturn404ForTypoInAuthEndpoint() throws Exception {

    final var body = "{\"username\":\"testuser\",\"password\":\"test1!\"}";
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/registe"))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    final var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(404, response.statusCode());
    assertTrue(response.body().contains("RESOURCE_NOT_FOUND"));
  }

  private Map<String, Object> loginAndParse(final String username) throws Exception {

    final var registerBody = "{\"username\":\"" + username + "\",\"password\":\"test1!\"}";
    this.httpClient.send(HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerBody)).build(),
        HttpResponse.BodyHandlers.ofString());

    final var loginBody = "{\"username\":\"" + username + "\",\"password\":\"test1!\"}";
    final var loginResponse = this.httpClient.send(
        HttpRequest.newBuilder(URI.create(this.baseUrl() + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody)).build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(200, loginResponse.statusCode());
    return this.readJson(loginResponse.body());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readJson(final String body) throws Exception {

    return this.objectMapper.readValue(body, Map.class);
  }

  private String baseUrl() {

    return "http://localhost:" + this.port;
  }

}
