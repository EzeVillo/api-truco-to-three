package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.CreateCupDTO;
import com.villo.truco.application.dto.CupStateDTO;
import com.villo.truco.application.dto.JoinCupDTO;
import com.villo.truco.application.dto.PublicCupLobbyDTO;
import com.villo.truco.application.ports.in.CreateCupUseCase;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.ports.in.GetPublicCupsUseCase;
import com.villo.truco.application.ports.in.JoinCupUseCase;
import com.villo.truco.application.ports.in.JoinPublicCupUseCase;
import com.villo.truco.application.ports.in.LeaveCupUseCase;
import com.villo.truco.application.ports.in.StartCupUseCase;
import com.villo.truco.domain.shared.pagination.CursorPageResult;
import com.villo.truco.domain.shared.valueobjects.Visibility;
import com.villo.truco.infrastructure.http.dto.request.CreateCupRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinCupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("CupController")
class CupControllerTest {

  private Jwt jwt(final String subject) {

    return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
  }

  @Test
  @DisplayName("create/join/start/get/leave")
  void flows() {

    final CreateCupUseCase create = mock(CreateCupUseCase.class);
    final JoinCupUseCase join = mock(JoinCupUseCase.class);
    final JoinPublicCupUseCase joinPublic = mock(JoinPublicCupUseCase.class);
    final LeaveCupUseCase leave = mock(LeaveCupUseCase.class);
    final StartCupUseCase start = mock(StartCupUseCase.class);
    final GetCupStateUseCase get = mock(GetCupStateUseCase.class);
    final GetPublicCupsUseCase getPublic = mock(GetPublicCupsUseCase.class);

    when(create.handle(any())).thenReturn(new CreateCupDTO("c1", "XYZ", "PRIVATE"));
    when(join.handle(any())).thenReturn(new JoinCupDTO("c1"));
    when(joinPublic.handle(any())).thenReturn(new JoinCupDTO("c1"));
    when(get.handle(any())).thenReturn(
        new CupStateDTO("c1", "WAITING_FOR_START", java.util.List.of(), null));
    when(getPublic.handle(any())).thenReturn(new CursorPageResult<>(
        java.util.List.of(new PublicCupLobbyDTO("c1", "juancho", 3, 8, 5, "WAITING_FOR_PLAYERS")),
        "cursor-1"));

    final var controller = new CupController(create, join, joinPublic, leave, start, get,
        getPublic);
    final var jwt = jwt("11111111-1111-1111-1111-111111111111");
    final var cupId = "44444444-4444-4444-4444-444444444444";

    assertThat(controller.createCup(new CreateCupRequest(4, 3, Visibility.PRIVATE.name()), jwt)
        .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.joinCup(new JoinCupRequest("XYZ"), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    final var publicCups = controller.getPublicCups(20, null, jwt);
    assertThat(publicCups.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(publicCups.getBody()).isNotNull();
    assertThat(publicCups.getBody().items()).hasSize(1);
    assertThat(publicCups.getBody()._links().self().href()).isEqualTo("/api/cups/public?limit=20");
    assertThat(publicCups.getBody()._links().next().href()).isEqualTo(
        "/api/cups/public?limit=20&after=cursor-1");
    assertThat(publicCups.getBody().items().getFirst()._links().joinPublic().href()).isEqualTo(
        "/api/cups/c1/join-public");
    assertThat(controller.joinPublicCup(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.startCup(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.leaveCup(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.getCupState(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
