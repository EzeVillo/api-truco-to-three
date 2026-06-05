package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.ActiveMatchRefDTO;
import com.villo.truco.application.dto.UserPresenceDTO;
import com.villo.truco.application.ports.in.GetUserPresenceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("PresenceController")
class PresenceControllerTest {

  private GetUserPresenceUseCase getUserPresence;
  private PresenceController controller;

  @BeforeEach
  void setUp() {

    getUserPresence = mock(GetUserPresenceUseCase.class);
    controller = new PresenceController(getUserPresence);
  }

  private Jwt jwt(final String subject) {

    return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
  }

  @Test
  @DisplayName("GET presence de usuario libre devuelve 200 con busy false y refs nulas")
  void freeUserReturnsNotBusy() {

    when(getUserPresence.handle(any())).thenReturn(UserPresenceDTO.of(null, null, null, null));

    final var response = controller.getPresence(jwt("11111111-1111-1111-1111-111111111111"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().busy()).isFalse();
    assertThat(response.getBody().match()).isNull();
    assertThat(response.getBody().league()).isNull();
    assertThat(response.getBody().cup()).isNull();
    assertThat(response.getBody().rematch()).isNull();
  }

  @Test
  @DisplayName("GET presence con partida activa mapea match ref y busy true")
  void busyUserMapsMatchReference() {

    when(getUserPresence.handle(any())).thenReturn(
        UserPresenceDTO.of(new ActiveMatchRefDTO("match-id", "IN_PROGRESS"), null, null, null));

    final var response = controller.getPresence(jwt("22222222-2222-2222-2222-222222222222"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().busy()).isTrue();
    assertThat(response.getBody().match()).isNotNull();
    assertThat(response.getBody().match().id()).isEqualTo("match-id");
    assertThat(response.getBody().match().status()).isEqualTo("IN_PROGRESS");
  }

}
