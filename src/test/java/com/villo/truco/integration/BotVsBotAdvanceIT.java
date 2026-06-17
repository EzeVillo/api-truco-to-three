package com.villo.truco.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.villo.truco.application.commands.SpectateMatchCommand;
import com.villo.truco.application.dto.SpectatorMatchStateDTO;
import com.villo.truco.application.ports.in.SpectateMatchUseCase;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import com.villo.truco.infrastructure.persistence.repositories.spring.SpringDataJoinCodeRegistryRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
@DisplayName("Bot vs Bot avance manual (integración)")
class BotVsBotAdvanceIT {

  private static final String BOT_ONE = "00000000-0000-0000-0000-000000000001";
  private static final String BOT_TWO = "00000000-0000-0000-0000-000000000002";

  private final HttpClient httpClient = HttpClient.newHttpClient();
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

    when(this.springDataJoinCodeRegistryRepository.insertIfAbsent(any(), any(), any())).thenReturn(
        1);
  }

  @Test
  @DisplayName("la partida no avanza sola; cada request del dueño la hace progresar")
  void doesNotAdvanceOnItsOwnAndProgressesPerRequest() throws Exception {

    final var owner = PlayerId.generate();
    final var token = this.tokenIssuer.issueForUser(owner).value();
    final var matchId = this.createMatch(token);

    final var initialVersion = this.stateOf(matchId, owner).stateVersion();

    // En modo automático ya habría avanzado varias acciones; confirmamos que NO lo hace.
    Thread.sleep(1500);
    assertThat(this.stateOf(matchId, owner).stateVersion()).isEqualTo(initialVersion);

    final var advance = this.post("/api/matches/bot-vs-bot/" + matchId + "/advance", token);
    assertThat(advance.statusCode()).isEqualTo(204);

    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(
        () -> assertThat(this.stateOf(matchId, owner).stateVersion()).isGreaterThan(
            initialVersion));
  }

  @Test
  @DisplayName("un usuario que no es el creador no puede avanzar la partida (422)")
  void nonOwnerCannotAdvance() throws Exception {

    final var owner = PlayerId.generate();
    final var ownerToken = this.tokenIssuer.issueForUser(owner).value();
    final var matchId = this.createMatch(ownerToken);

    final var strangerToken = this.tokenIssuer.issueForUser(PlayerId.generate()).value();

    final var advance = this.post("/api/matches/bot-vs-bot/" + matchId + "/advance", strangerToken);
    assertThat(advance.statusCode()).isEqualTo(422);
  }

  private SpectatorMatchStateDTO stateOf(final String matchId, final PlayerId owner) {

    return this.spectateMatch.handle(new SpectateMatchCommand(matchId, owner.value().toString()));
  }

  private String createMatch(final String token) throws Exception {

    final var response = this.post("/api/matches/bot-vs-bot",
        "{\"botOneId\":\"" + BOT_ONE + "\",\"botTwoId\":\"" + BOT_TWO + "\",\"gamesToPlay\":5}",
        token);
    assertThat(response.statusCode()).isEqualTo(200);
    return response.body().replaceAll(".*\"matchId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
  }

  private HttpResponse<String> post(final String path, final String token) throws Exception {

    return this.post(path, null, token);
  }

  private HttpResponse<String> post(final String path, final String body, final String token)
      throws Exception {

    final var bodyPublisher = body == null ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body);
    final var request = HttpRequest.newBuilder(URI.create(this.baseUrl() + path))
        .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
        .POST(bodyPublisher).build();
    return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String baseUrl() {

    return "http://localhost:" + this.port;
  }

}
