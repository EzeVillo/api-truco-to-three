package com.villo.truco.integration;

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
@DisplayName("Bot vs Bot ocupación por autoría (integración)")
class BotVsBotOccupancyIT {

  private static final String BOT_ONE = "00000000-0000-0000-0000-000000000001";
  private static final String BOT_TWO = "00000000-0000-0000-0000-000000000002";
  private static final String BOT_THREE = "00000000-0000-0000-0000-000000000003";

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

    when(this.springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  @Test
  @DisplayName("crear una bot-match deja al creador busy con ownedBotMatch sin estar espectando")
  void ownerBecomesBusyByAuthorship() throws Exception {

    final var owner = PlayerId.generate();
    final var token = this.tokenIssuer.issueForUser(owner).value();

    final var createResponse = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_TWO + "\",\"gamesToPlay\":5}",
        token);
    assertThat(createResponse.statusCode()).isEqualTo(200);
    final var matchId = (String) this.readJson(createResponse.body()).get("matchId");

    final var presence = this.get("/api/me/presence", token);
    assertThat(presence.statusCode()).isEqualTo(200);
    final var body = this.readJson(presence.body());
    assertThat(body.get("busy")).isEqualTo(Boolean.TRUE);
    assertThat(body.get("spectating")).isNull();
    final var ownedBotMatch = body.get("ownedBotMatch");
    assertThat(ownedBotMatch).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked") final var owned = (Map<String, Object>) ownedBotMatch;
    assertThat(owned.get("matchId")).isEqualTo(matchId);
    assertThat(owned.get("status")).isEqualTo("IN_PROGRESS");
  }

  @Test
  @DisplayName("con una bot-match propia activa, crear otra bot-match o una partida normal devuelve 422")
  void busyOwnerCannotStartNewActivities() throws Exception {

    final var owner = PlayerId.generate();
    final var token = this.tokenIssuer.issueForUser(owner).value();

    final var first = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_TWO + "\",\"gamesToPlay\":5}",
        token);
    assertThat(first.statusCode()).isEqualTo(200);

    final var anotherBotVsBot = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_THREE + "\",\"gamesToPlay\":5}",
        token);
    assertThat(anotherBotVsBot.statusCode()).isEqualTo(422);

    final var normalBotMatch = this.post("/api/matches/bot",
        "{\"gamesToPlay\":3,\"botId\":\"" + BOT_THREE + "\"}", token);
    assertThat(normalBotMatch.statusCode()).isEqualTo(422);
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
