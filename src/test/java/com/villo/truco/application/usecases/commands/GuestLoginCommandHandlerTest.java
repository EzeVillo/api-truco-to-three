package com.villo.truco.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.application.commands.GuestLoginCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GuestLoginCommandHandler")
class GuestLoginCommandHandlerTest {

  @Test
  @DisplayName("genera playerId efímero y devuelve token")
  void generatesEphemeralPlayerIdAndToken() {

    final var handler = new GuestLoginCommandHandler(playerId -> "token-" + playerId.value());

    final var dto = handler.handle(new GuestLoginCommand());

    assertThat(dto.playerId()).isNotNull();
    assertThat(dto.accessToken()).isNotNull();
  }

  @Test
  @DisplayName("dos llamadas generan playerIds distintos")
  void eachCallGeneratesUniquePlayerId() {

    final var handler = new GuestLoginCommandHandler(playerId -> "token-" + playerId.value());

    final var dto1 = handler.handle(new GuestLoginCommand());
    final var dto2 = handler.handle(new GuestLoginCommand());

    assertThat(dto1.playerId()).isNotEqualTo(dto2.playerId());
  }

}
