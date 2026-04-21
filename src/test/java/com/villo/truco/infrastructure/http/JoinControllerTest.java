package com.villo.truco.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.dto.JoinResourceDTO;
import com.villo.truco.application.ports.in.JoinByCodeUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("JoinController")
class JoinControllerTest {

  private Jwt jwt(final String subject) {

    return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
  }

  @Test
  @DisplayName("resuelve join global por joinCode")
  void joinsByCode() {

    final JoinByCodeUseCase joinByCodeUseCase = mock(JoinByCodeUseCase.class);
    when(joinByCodeUseCase.handle(any())).thenReturn(new JoinResourceDTO("MATCH", "m1"));

    final var controller = new JoinController(joinByCodeUseCase);
    final var response = controller.join("ABCD1234", jwt("11111111-1111-1111-1111-111111111111"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().targetType()).isEqualTo("MATCH");
    assertThat(response.getBody().targetId()).isEqualTo("m1");
  }

}
