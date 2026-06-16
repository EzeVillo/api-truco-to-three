package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.ports.in.SpectateMatchUseCase;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.model.spectator.exceptions.SpectateBotMatchNotOwnerException;
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
@DisplayName("Bot vs Bot espectable (integración)")
class BotVsBotSpectatingIT {

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
  @Autowired
  private SpectateMatchUseCase spectateMatch;

  @BeforeEach
  void setUp() {

    when(this.springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(1);
  }

  private String createMatch(final PlayerId owner) throws Exception {

    final var token = this.tokenIssuer.issueForUser(owner).value();
    final var response = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_TWO + "\",\"gamesToPlay\":5}",
        token);
    assertThat(response.statusCode()).isEqualTo(200);
    return (String) this.readJson(response.body()).get("matchId");
  }

  @Test
  @DisplayName("el creador espectea y recibe las manos de ambos bots; un no-creador es rechazado")
  void createSpectateAndOwnerOnly() throws Exception {

    final var owner = PlayerId.generate();
    final var matchId = this.createMatch(owner);
    assertThat(matchId).isNotBlank();

    final var state = this.spectateMatch.handle(new SpectateMatchCommand(matchId,
        owner.value().toString()));
    assertThat(state.currentRound()).isNotNull();
    assertThat(state.currentRound().handPlayerOne()).isNotNull();
    assertThat(state.currentRound().handPlayerTwo()).isNotNull();

    final var intruder = PlayerId.generate();
    assertThatThrownBy(() -> this.spectateMatch.handle(
        new SpectateMatchCommand(matchId, intruder.value().toString()))).isInstanceOf(
        SpectateBotMatchNotOwnerException.class);
  }

  @Test
  @DisplayName("la partida es PRIVATE: no aparece en el lobby público")
  void matchIsPrivateAndOutOfLobby() throws Exception {

    final var owner = PlayerId.generate();
    final var matchId = this.createMatch(owner);

    // El lobby se consulta con un usuario libre: el creador queda busy total y no puede listarlo.
    final var viewerToken = this.tokenIssuer.issueForUser(PlayerId.generate()).value();
    final var lobby = this.get("/api/matches/public", viewerToken);
    assertThat(lobby.statusCode()).isEqualTo(200);
    assertThat(lobby.body()).doesNotContain(matchId);
  }

  @Test
  @DisplayName("crear con bots iguales devuelve 422")
  void rejectsSameBot() throws Exception {

    final var owner = PlayerId.generate();
    final var ownerToken = this.tokenIssuer.issueForUser(owner).value();

    final var response = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_ONE + "\",\"gamesToPlay\":3}",
        ownerToken);

    assertThat(response.statusCode()).isEqualTo(422);
  }

  @Test
  @DisplayName("crear con un bot inexistente devuelve 404")
  void rejectsUnknownBot() throws Exception {

    final var owner = PlayerId.generate();
    final var ownerToken = this.tokenIssuer.issueForUser(owner).value();

    final var response = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + PlayerId.generate().value()
            + "\",\"gamesToPlay\":3}", ownerToken);

    assertThat(response.statusCode()).isEqualTo(404);
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
