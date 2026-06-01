package com.villo.truco.auth.application.usecases.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.application.exceptions.UnauthorizedAccessException;
import com.villo.truco.auth.application.queries.GetCurrentSessionIdentityQuery;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GetCurrentSessionIdentityQueryHandler")
class GetCurrentSessionIdentityQueryHandlerTest {

  @Test
  @DisplayName("devuelve username para token de usuario registrado")
  void returnsRegisteredUserIdentity() {

    final var playerId = PlayerId.generate();
    final var users = mock(UserQueryRepository.class);
    when(users.findUsernameById(playerId)).thenReturn(Optional.of("juancho"));
    final var handler = new GetCurrentSessionIdentityQueryHandler(users);

    final var identity = handler.handle(
        new GetCurrentSessionIdentityQuery(playerId.value().toString(), "user"));

    assertThat(identity.playerId()).isEqualTo(playerId);
    assertThat(identity.username()).isEqualTo("juancho");
    assertThat(identity.tokenUse()).isEqualTo("user");
  }

  @Test
  @DisplayName("devuelve identidad guest sin username")
  void returnsGuestIdentityWithoutUsername() {

    final var playerId = PlayerId.generate();
    final var handler = new GetCurrentSessionIdentityQueryHandler(mock(UserQueryRepository.class));

    final var identity = handler.handle(
        new GetCurrentSessionIdentityQuery(playerId.value().toString(), "guest"));

    assertThat(identity.playerId()).isEqualTo(playerId);
    assertThat(identity.username()).isNull();
    assertThat(identity.tokenUse()).isEqualTo("guest");
  }

  @Test
  @DisplayName("rechaza token de usuario registrado si el usuario no existe")
  void rejectsMissingRegisteredUser() {

    final var playerId = PlayerId.generate();
    final var users = mock(UserQueryRepository.class);
    when(users.findUsernameById(playerId)).thenReturn(Optional.empty());
    final var handler = new GetCurrentSessionIdentityQueryHandler(users);

    assertThatThrownBy(() -> handler.handle(
        new GetCurrentSessionIdentityQuery(playerId.value().toString(), "user"))).isInstanceOf(
        UnauthorizedAccessException.class);
  }

  @Test
  @DisplayName("rechaza tokenUse desconocido")
  void rejectsUnknownTokenUse() {

    final var handler = new GetCurrentSessionIdentityQueryHandler(mock(UserQueryRepository.class));

    assertThatThrownBy(() -> handler.handle(
        new GetCurrentSessionIdentityQuery(PlayerId.generate().value().toString(),
            "unknown"))).isInstanceOf(UnauthorizedAccessException.class);
  }

}
