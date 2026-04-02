package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.auth.application.commands.GuestLoginCommand;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GuestLoginCommandHandler")
class GuestLoginCommandHandlerTest {

  private final AccessTokenIssuer accessTokenIssuer = new AccessTokenIssuer() {

    @Override
    public IssuedAccessToken issueForUser(final PlayerId playerId) {

      throw new UnsupportedOperationException();
    }

    @Override
    public IssuedAccessToken issueForGuest(final PlayerId playerId) {

      return new IssuedAccessToken("guest-" + playerId.value(), 604800);
    }
  };

  @Test
  @DisplayName("genera playerId efimero y devuelve token")
  void generatesEphemeralPlayerIdAndToken() {

    final var handler = new GuestLoginCommandHandler(this.accessTokenIssuer);

    final var session = handler.handle(new GuestLoginCommand());

    assertThat(session.playerId()).isNotNull();
    assertThat(session.accessToken()).startsWith("guest-");
    assertThat(session.accessTokenExpiresIn()).isEqualTo(604800);
  }

  @Test
  @DisplayName("dos llamadas generan playerIds distintos")
  void eachCallGeneratesUniquePlayerId() {

    final var handler = new GuestLoginCommandHandler(this.accessTokenIssuer);

    final var session1 = handler.handle(new GuestLoginCommand());
    final var session2 = handler.handle(new GuestLoginCommand());

    assertThat(session1.playerId()).isNotEqualTo(session2.playerId());
  }

}
