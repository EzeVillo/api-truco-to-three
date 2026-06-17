package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
import java.time.Duration;
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
@DisplayName("Bot vs Bot liberación al terminar (integración)")
class BotVsBotReleaseIT {

  private static final String BOT_ONE = "00000000-0000-0000-0000-000000000001";
  private static final String BOT_TWO = "00000000-0000-0000-0000-000000000002";

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

    when(this.springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(
        1);
  }

  @Test
  @DisplayName("al terminar la partida (los bots juegan solos) el creador queda libre y puede crear otra")
  void ownerIsReleasedWhenMatchFinishes() throws Exception {

    final var owner = PlayerId.generate();
    final var token = this.tokenIssuer.issueForUser(owner).value();

    // gamesToPlay=1: la serie es la más corta posible, así los bots la terminan rápido solos.
    final var createResponse = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_TWO + "\",\"gamesToPlay\":1}",
        token);
    assertThat(createResponse.statusCode()).isEqualTo(200);

    final var busy = this.readJson(this.get("/api/me/presence", token).body());
    assertThat(busy.get("busy")).isEqualTo(Boolean.TRUE);
    assertThat(busy.get("ownedBotMatch")).isInstanceOf(Map.class);

    // La liberación es emergente del filtro de estado: al terminar la partida, ownedBotMatch se vacía.
    await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          final var presence = this.readJson(this.get("/api/me/presence", token).body());
          assertThat(presence.get("ownedBotMatch")).isNull();
          assertThat(presence.get("busy")).isEqualTo(Boolean.FALSE);
        });

    final var newMatch = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_TWO + "\",\"gamesToPlay\":1}",
        token);
    assertThat(newMatch.statusCode()).isEqualTo(200);
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
