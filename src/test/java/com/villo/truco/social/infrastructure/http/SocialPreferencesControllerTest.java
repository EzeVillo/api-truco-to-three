package com.villo.truco.social.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.villo.truco.social.application.commands.UpdateSocialPreferencesCommand;
import com.villo.truco.social.application.dto.SocialPreferencesDTO;
import com.villo.truco.social.application.ports.in.GetSocialPreferencesUseCase;
import com.villo.truco.social.application.ports.in.UpdateSocialPreferencesUseCase;
import com.villo.truco.social.infrastructure.http.dto.request.UpdateSocialPreferencesRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("SocialPreferencesController")
class SocialPreferencesControllerTest {

  private static final String ACTOR_ID = "11111111-1111-1111-1111-111111111111";

  private static Jwt jwt() {

    return Jwt.withTokenValue("token").header("alg", "none").subject(ACTOR_ID).build();
  }

  @Test
  @DisplayName("GET devuelve las preferencias del usuario autenticado")
  void getReturnsPreferences() {

    final GetSocialPreferencesUseCase getUseCase = mock(GetSocialPreferencesUseCase.class);
    final UpdateSocialPreferencesUseCase updateUseCase = mock(UpdateSocialPreferencesUseCase.class);
    when(getUseCase.handle(any())).thenReturn(new SocialPreferencesDTO(false));

    final var controller = new SocialPreferencesController(getUseCase, updateUseCase);
    final var response = controller.getMyPreferences(jwt());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().acceptsFriendRequests()).isFalse();
  }

  @Test
  @DisplayName("PUT actualiza las preferencias y propaga el subject del token")
  void putUpdatesPreferences() {

    final GetSocialPreferencesUseCase getUseCase = mock(GetSocialPreferencesUseCase.class);
    final UpdateSocialPreferencesUseCase updateUseCase = mock(UpdateSocialPreferencesUseCase.class);
    when(updateUseCase.handle(any())).thenReturn(new SocialPreferencesDTO(false));

    final var controller = new SocialPreferencesController(getUseCase, updateUseCase);
    final var response = controller.updateMyPreferences(new UpdateSocialPreferencesRequest(false),
        jwt());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().acceptsFriendRequests()).isFalse();

    final var captor = ArgumentCaptor.forClass(UpdateSocialPreferencesCommand.class);
    verify(updateUseCase).handle(captor.capture());
    assertThat(captor.getValue().playerId().value().toString()).isEqualTo(ACTOR_ID);
    assertThat(captor.getValue().acceptsFriendRequests()).isFalse();
  }

}
