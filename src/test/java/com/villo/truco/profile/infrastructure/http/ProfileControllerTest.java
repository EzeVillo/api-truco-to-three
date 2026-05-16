package com.villo.truco.profile.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.profile.application.dto.PlayerProfileDTO;
import com.villo.truco.profile.application.dto.PlayerStatsDTO;
import com.villo.truco.profile.application.usecases.queries.GetPlayerProfileUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("ProfileController")
class ProfileControllerTest {

  @Test
  @DisplayName("GET /{username} devuelve 200 con el perfil del jugador")
  void getProfileReturns200() {

    final var useCase = mock(GetPlayerProfileUseCase.class);
    final var controller = new ProfileController(useCase);
    final var dto = new PlayerProfileDTO(List.of(), new PlayerStatsDTO(10, 6, 4, 60));
    when(useCase.handle(any())).thenReturn(dto);
    final var jwt = Jwt.withTokenValue("token").header("alg", "none")
        .subject(UUID.randomUUID().toString()).build();

    final var response = controller.getProfile("juancho", jwt);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().stats().matchesPlayed()).isEqualTo(10);
    assertThat(response.getBody().stats().matchesWon()).isEqualTo(6);
    assertThat(response.getBody().stats().winRate()).isEqualTo(60);
    assertThat(response.getBody().achievements()).isEmpty();
  }

}
