package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.CreateLeagueDTO;
import com.villo.truco.application.dto.JoinLeagueDTO;
import com.villo.truco.application.dto.LeagueStateDTO;
import com.villo.truco.application.ports.in.CreateLeagueUseCase;
import com.villo.truco.application.ports.in.GetLeagueStateUseCase;
import com.villo.truco.application.ports.in.JoinLeagueUseCase;
import com.villo.truco.application.ports.in.LeaveLeagueUseCase;
import com.villo.truco.application.ports.in.StartLeagueUseCase;
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
    final LeaveLeagueUseCase leave = mock(LeaveLeagueUseCase.class);
    final StartLeagueUseCase start = mock(StartLeagueUseCase.class);
    final GetLeagueStateUseCase get = mock(GetLeagueStateUseCase.class);

    when(create.handle(any())).thenReturn(new CreateLeagueDTO("l1", "XYZ"));
    when(join.handle(any())).thenReturn(new JoinLeagueDTO("l1"));
    when(get.handle(any())).thenReturn(
        new LeagueStateDTO("l1", "WAITING_FOR_START", List.of(), List.of(), List.of()));

    final var controller = new LeagueController(create, join, leave, start, get);
    final var jwt = jwt("11111111-1111-1111-1111-111111111111");
    final var leagueId = "33333333-3333-3333-3333-333333333333";

    assertThat(
        controller.createLeague(new CreateLeagueRequest(3, 3), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.joinLeague(new JoinLeagueRequest("XYZ"), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.startLeague(leagueId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.leaveLeague(leagueId, jwt).getStatusCode()).isEqualTo(
        HttpStatus.NO_CONTENT);
    assertThat(controller.getLeagueState(leagueId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
