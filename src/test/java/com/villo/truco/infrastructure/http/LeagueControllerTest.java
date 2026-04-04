package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.CreateLeagueDTO;
import com.villo.truco.application.dto.JoinLeagueDTO;
import com.villo.truco.application.dto.LeagueStateDTO;
import com.villo.truco.application.dto.PublicLeagueLobbyDTO;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.GetPublicLeaguesUseCase;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.application.ports.in.JoinPublicLeagueUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.http.dto.request.CreateLeagueRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinLeagueRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("LeagueController")
class LeagueControllerTest {

  private Jwt jwt(final String subject) {

    return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
  }

  @Test
  @DisplayName("create/join/start/get/leave")
  void flows() {

    final CreateLeagueUseCase create = mock(CreateLeagueUseCase.class);
    final JoinLeagueUseCase join = mock(JoinLeagueUseCase.class);
    final JoinPublicLeagueUseCase joinPublic = mock(JoinPublicLeagueUseCase.class);
    final LeaveLeagueUseCase leave = mock(LeaveLeagueUseCase.class);
    final StartLeagueUseCase start = mock(StartLeagueUseCase.class);
    final GetLeagueStateUseCase get = mock(GetLeagueStateUseCase.class);
    final GetPublicLeaguesUseCase getPublic = mock(GetPublicLeaguesUseCase.class);

    when(create.handle(any())).thenReturn(new CreateLeagueDTO("l1", "XYZ", "PRIVATE"));
    when(join.handle(any())).thenReturn(new JoinLeagueDTO("l1"));
    when(joinPublic.handle(any())).thenReturn(new JoinLeagueDTO("l1"));
    when(get.handle(any())).thenReturn(
        new LeagueStateDTO("l1", "WAITING_FOR_START", List.of(), List.of(), List.of()));
    when(getPublic.handle(any())).thenReturn(new CursorPageResult<>(
        List.of(new PublicLeagueLobbyDTO("l1", "juancho", 3, 4, 2, "WAITING_FOR_PLAYERS")),
        "cursor-1"));

    final var controller = new LeagueController(create, join, joinPublic, leave, start, get,
        getPublic);
    final var jwt = jwt("11111111-1111-1111-1111-111111111111");
    final var leagueId = "33333333-3333-3333-3333-333333333333";

    assertThat(
        controller.createLeague(new CreateLeagueRequest(3, 3, Visibility.PRIVATE.name()), jwt)
            .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.joinLeague(new JoinLeagueRequest("XYZ"), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    final var publicLeagues = controller.getPublicLeagues(20, null, jwt);
    assertThat(publicLeagues.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(publicLeagues.getBody()).isNotNull();
    assertThat(publicLeagues.getBody().items()).hasSize(1);
    assertThat(publicLeagues.getBody()._links().self().href()).isEqualTo(
        "/api/leagues/public?limit=20");
    assertThat(publicLeagues.getBody()._links().next().href()).isEqualTo(
        "/api/leagues/public?limit=20&after=cursor-1");
    assertThat(publicLeagues.getBody().items().getFirst()._links().joinPublic().href()).isEqualTo(
        "/api/leagues/l1/join-public");
    assertThat(controller.joinPublicLeague(leagueId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.startLeague(leagueId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.leaveLeague(leagueId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.getLeagueState(leagueId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
