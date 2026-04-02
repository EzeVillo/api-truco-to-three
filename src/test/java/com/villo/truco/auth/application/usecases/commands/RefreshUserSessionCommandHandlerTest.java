package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.villo.truco.auth.application.commands.RefreshUserSessionCommand;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.exceptions.InvalidUserSessionRefreshException;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshUserSessionCommandHandler")
class RefreshUserSessionCommandHandlerTest {

  @Test
  @DisplayName("rota refresh token y revoca la cadena si se reusa el token viejo")
  void rotatesAndRevokesChainOnReplay() {

    final var playerId = PlayerId.generate();
    final var repository = new AuthTestFixtures.InMemoryUserSessionRepository();
    final var refreshTokenProvider = new AuthTestFixtures.SequencedRefreshTokenProvider(1);
    final var current = UserSession.issue(playerId, refreshTokenProvider.hash("refresh-0"),
        AuthTestFixtures.NOW,
        AuthTestFixtures.NOW.plusSeconds(AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN));
    repository.save(current);
    final var issuer = new UserSessionIssuer(AuthTestFixtures.stubAccessTokenIssuer(),
        refreshTokenProvider, repository, AuthTestFixtures.fixedClock());

    final var handler = new RefreshUserSessionCommandHandler(repository, refreshTokenProvider,
        issuer);

    final var refreshed = handler.handle(new RefreshUserSessionCommand("refresh-0"));

    assertThat(refreshed.refreshToken()).isEqualTo("refresh-1");
    final var rotatedSession = repository.findByRefreshTokenHash(
        refreshTokenProvider.hash("refresh-0")).orElseThrow();
    final var rotated = rotatedSession.refreshTokenSnapshots().get(0);
    final var replacement = rotatedSession.refreshTokenSnapshots().get(1);
    assertThat(rotated.rotatedAt()).isEqualTo(AuthTestFixtures.NOW);
    assertThat(rotated.replacedByTokenId()).isEqualTo(replacement.id());

    assertThatThrownBy(
        () -> handler.handle(new RefreshUserSessionCommand("refresh-0"))).isInstanceOf(
        InvalidUserSessionRefreshException.class);

    assertThat(repository.findById(rotatedSession.getId()).orElseThrow().isRevoked()).isTrue();
    assertThatThrownBy(
        () -> handler.handle(new RefreshUserSessionCommand("refresh-1"))).isInstanceOf(
        InvalidUserSessionRefreshException.class);
  }

}
