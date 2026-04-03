package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.CreateMatchDTO;
import com.villo.truco.application.dto.JoinMatchDTO;
import com.villo.truco.application.dto.MatchStateDTO;
import com.villo.truco.application.ports.in.AbandonMatchUseCase;
import com.villo.truco.application.ports.in.CallEnvidoUseCase;
import com.villo.truco.application.ports.in.CallTrucoUseCase;
import com.villo.truco.application.ports.in.CreateMatchUseCase;
import com.villo.truco.application.ports.in.FoldUseCase;
import com.villo.truco.application.ports.in.GetMatchStateUseCase;
import com.villo.truco.application.ports.in.GetSpectateMatchStateUseCase;
import com.villo.truco.application.ports.in.JoinMatchUseCase;
import com.villo.truco.application.ports.in.PlayCardUseCase;
import com.villo.truco.application.ports.in.RespondEnvidoUseCase;
import com.villo.truco.application.ports.in.RespondTrucoUseCase;
import com.villo.truco.application.ports.in.StartMatchUseCase;
import com.villo.truco.infrastructure.http.dto.request.CreateMatchRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinMatchRequest;
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
    final StartMatchUseCase start = mock(StartMatchUseCase.class);
    final PlayCardUseCase play = mock(PlayCardUseCase.class);
    final CallTrucoUseCase truco = mock(CallTrucoUseCase.class);
    final RespondTrucoUseCase respondTruco = mock(RespondTrucoUseCase.class);
    final CallEnvidoUseCase envido = mock(CallEnvidoUseCase.class);
    final RespondEnvidoUseCase respondEnvido = mock(RespondEnvidoUseCase.class);
    final FoldUseCase fold = mock(FoldUseCase.class);
    final AbandonMatchUseCase abandon = mock(AbandonMatchUseCase.class);
    final GetMatchStateUseCase get = mock(GetMatchStateUseCase.class);

    when(create.handle(any())).thenReturn(new CreateMatchDTO("m1", "ABCD"));
    when(join.handle(any())).thenReturn(new JoinMatchDTO("m1"));
    when(get.handle(any())).thenReturn(mock(MatchStateDTO.class));

    final GetSpectateMatchStateUseCase getSpectate = mock(GetSpectateMatchStateUseCase.class);
    final var controller = new MatchController(create, join, start, play, truco, respondTruco,
        envido, respondEnvido, fold, abandon, get, getSpectate);
    final var jwt = jwt("11111111-1111-1111-1111-111111111111");
    final var matchId = "22222222-2222-2222-2222-222222222222";

    assertThat(controller.createMatch(new CreateMatchRequest(3), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.joinMatch(new JoinMatchRequest("ABCD"), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.getMatchState(matchId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.startMatch(matchId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.callTruco(matchId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.fold(matchId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.abandonMatch(matchId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
  }

}
