package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.CreateCupDTO;
import com.villo.truco.application.dto.CupStateDTO;
import com.villo.truco.application.dto.JoinCupDTO;
import com.villo.truco.application.ports.in.CreateCupUseCase;
import com.villo.truco.application.ports.in.GetCupStateUseCase;
import com.villo.truco.application.ports.in.JoinCupUseCase;
import com.villo.truco.application.ports.in.LeaveCupUseCase;
import com.villo.truco.application.ports.in.StartCupUseCase;
import com.villo.truco.infrastructure.http.dto.request.CreateCupRequest;
import com.villo.truco.infrastructure.http.dto.request.JoinCupRequest;
import java.util.List;
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
    final LeaveCupUseCase leave = mock(LeaveCupUseCase.class);
    final StartCupUseCase start = mock(StartCupUseCase.class);
    final GetCupStateUseCase get = mock(GetCupStateUseCase.class);

    when(create.handle(any())).thenReturn(new CreateCupDTO("c1", "XYZ"));
    when(join.handle(any())).thenReturn(new JoinCupDTO("c1"));
    when(get.handle(any())).thenReturn(new CupStateDTO("c1", "WAITING_FOR_START", List.of(), null));

    final var controller = new CupController(create, join, leave, start, get);
    final var jwt = jwt("11111111-1111-1111-1111-111111111111");
    final var cupId = "44444444-4444-4444-4444-444444444444";

    assertThat(controller.createCup(new CreateCupRequest(4, 3), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.joinCup(new JoinCupRequest("XYZ"), jwt).getStatusCode()).isEqualTo(
        HttpStatus.OK);
    assertThat(controller.startCup(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.leaveCup(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(controller.getCupState(cupId, jwt).getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
