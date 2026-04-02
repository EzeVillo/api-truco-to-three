package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.auth.application.commands.LogoutUserSessionCommand;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LogoutUserSessionCommandHandler")
class LogoutUserSessionCommandHandlerTest {

  @Test
  @DisplayName("revoca solo la sesion del refresh token enviado")
  void revokesOnlyMatchingSession() {

    final var repository = new AuthTestFixtures.InMemoryUserSessionRepository();
    final var provider = AuthTestFixtures.constantRefreshTokenProvider("refresh");
    final var playerId = PlayerId.generate();
    final var sessionA = UserSession.issue(playerId, provider.hash("refresh-a"),
        AuthTestFixtures.NOW,
        AuthTestFixtures.NOW.plusSeconds(AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN));
    final var sessionB = UserSession.issue(playerId, provider.hash("refresh-b"),
        AuthTestFixtures.NOW,
        AuthTestFixtures.NOW.plusSeconds(AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN));
    repository.save(sessionA);
    repository.save(sessionB);

    final var handler = new LogoutUserSessionCommandHandler(repository, provider,
        AuthTestFixtures.fixedClock());

    handler.handle(new LogoutUserSessionCommand("refresh-a"));

    assertThat(repository.findById(sessionA.getId()).orElseThrow().isRevoked()).isTrue();
    assertThat(repository.findById(sessionB.getId()).orElseThrow().isRevoked()).isFalse();
  }

}
