package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {"truco.security.jwt-secret=test-secret-test-secret-test-secret",
    "truco.security.issuer=test-issuer", "truco.security.audience=test-audience",
    "spring.profiles.active=test"}, webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("MatchController snapshot version")
class MatchControllerSnapshotVersionTest {

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();
  @MockitoBean
  private SpringDataJoinCodeRegistryRepository springDataJoinCodeRegistryRepository;
  @LocalServerPort
  private int port;
  @Autowired
  private AccessTokenIssuer tokenIssuer;

  @BeforeEach
  void setUp() {

    when(springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("GET /api/matches/{matchId} devuelve stateVersion consistente con transiciones")
  void snapshotIncludesStateVersion() throws Exception {

    final var playerOne = PlayerId.generate();
    final var tokenOne = this.tokenIssuer.issueForUser(playerOne).value();

    final var createResponse = this.post("/api/matches",
        "{\"gamesToPlay\":3,\"visibility\":\"PRIVATE\"}", tokenOne);
    assertThat(createResponse.statusCode()).isEqualTo(200);
    final var created = this.readJson(createResponse.body());
    final var matchId = (String) created.get("matchId");

    final var snapshotResponse = this.get("/api/matches/" + matchId, tokenOne);
    assertThat(snapshotResponse.statusCode()).isEqualTo(200);
    final var snapshot = this.readJson(snapshotResponse.body());

    assertThat(snapshot).containsKey("stateVersion");
    assertThat(((Number) snapshot.get("stateVersion")).longValue()).isZero();
  }

  private HttpResponse<String> get(final String path, final String token) throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + path))
        .header("Authorization", "Bearer " + token).GET().build();
    return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(final String path, final String body, final String token)
      throws Exception {

    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + path))
        .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readJson(final String body) throws Exception {

    return this.objectMapper.readValue(body, Map.class);
  }

  private String baseUrl() {

    return "http://localhost:" + this.port;
  }

}
