package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.dto.JoinMatchDTO;
import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.application.dto.PublicMatchLobbyDTO;
import com.villo.truco.application.dto.RoundStateDTO;
import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GetPublicMatchesUseCase;
import com.villo.truco.application.ports.in.GetSpectateMatchStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.JoinPublicMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.http.dto.request.CreateMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinMatchRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("MatchController")
class MatchControllerTest {

  private Jwt jwt(final String subject) {

    return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
  }

  @Test
  @DisplayName("endpoints devuelven códigos esperados")
  void flows() {

    final CreateMatchUseCase create = mock(CreateMatchUseCase.class);
    final JoinMatchUseCase join = mock(JoinMatchUseCase.class);
    final JoinPublicMatchUseCase joinPublic = mock(JoinPublicMatchUseCase.class);
    final StartMatchUseCase start = mock(StartMatchUseCase.class);
    final PlayCardUseCase play = mock(PlayCardUseCase.class);
    final CallTrucoUseCase truco = mock(CallTrucoUseCase.class);
    final RespondTrucoUseCase respondTruco = mock(RespondTrucoUseCase.class);
    final CallEnvidoUseCase envido = mock(CallEnvidoUseCase.class);
    final RespondEnvidoUseCase respondEnvido = mock(RespondEnvidoUseCase.class);
    final FoldUseCase fold = mock(FoldUseCase.class);
    final AbandonMatchUseCase abandon = mock(AbandonMatchUseCase.class);
    final GetMatchStateUseCase get = mock(GetMatchStateUseCase.class);
    final GetPublicMatchesUseCase getPublic = mock(GetPublicMatchesUseCase.class);

    when(create.handle(any())).thenReturn(new CreateMatchDTO("m1", "ABCD", "PRIVATE"));
    when(join.handle(any())).thenReturn(new JoinMatchDTO("m1"));
    when(joinPublic.handle(any())).thenReturn(new JoinMatchDTO("m1"));
    when(get.handle(any())).thenReturn(new MatchStateDTO("m1", "IN_PROGRESS", 2, 1, 1, 0, null,
        new RoundStateDTO("IN_PROGRESS", "juancho", List.of(), "PLAYING", "TRUCO", null, List.of(),
            List.of(), null)));
    when(getPublic.handle(any())).thenReturn(new CursorPageResult<>(
        List.of(new PublicMatchLobbyDTO("m1", "juancho", 3, 2, 1, "WAITING_FOR_PLAYERS")),
        "cursor-1"));

    final GetSpectateMatchStateUseCase getSpectate = mock(GetSpectateMatchStateUseCase.class);
    final var controller = new MatchController(create, join, joinPublic, start, play, truco,
        respondTruco, envido, respondEnvido, fold, abandon, get, getPublic, getSpectate);
    final var jwt = jwt("11111111-1111-1111-1111-111111111111");
    final var matchId = "22222222-2222-2222-2222-222222222222";

    assertThat(controller.createMatch(new CreateMatchRequest(3, Visibility.PRIVATE.name()), jwt)
        .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.joinMatch(new JoinMatchRequest("ABCD"), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    final var publicMatches = controller.getPublicMatches(20, null, jwt);
    assertThat(publicMatches.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(publicMatches.getBody()).isNotNull();
    assertThat(publicMatches.getBody().items()).hasSize(1);
    assertThat(publicMatches.getBody()._links().self().href()).isEqualTo(
        "/api/matches/public?limit=20");
    assertThat(publicMatches.getBody()._links().next().href()).isEqualTo(
        "/api/matches/public?limit=20&after=cursor-1");
    assertThat(publicMatches.getBody().items().getFirst()._links().joinPublic().href()).isEqualTo(
        "/api/matches/m1/join-public");
    assertThat(controller.joinPublicMatch(matchId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
    final var stateResponse = controller.getMatchState(matchId, jwt);
    assertThat(stateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(stateResponse.getBody()).isNotNull();
    assertThat(stateResponse.getBody().scorePlayerOne()).isEqualTo(2);
    assertThat(stateResponse.getBody().scorePlayerTwo()).isEqualTo(1);
    assertThat(stateResponse.getBody().roundGame()).isNotNull();
    assertThat(controller.startMatch(matchId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.callTruco(matchId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.fold(matchId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.abandonMatch(matchId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
  }

}
