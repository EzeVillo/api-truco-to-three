package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.application.commands.RefreshUserSessionCommand;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.exceptions.InvalidUserSessionRefreshException;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshUserSessionCommandHandler")
class RefreshUserSessionCommandHandlerTest {

  @Test
  @DisplayName("rota refresh token y revoca la cadena si se reusa el token viejo")
  void rotatesAndRevokesChainOnReplay() {

    final var playerId = PlayerId.generate();
    final var sessions = new ConcurrentHashMap<UserSessionId, UserSession>();
    final var repository = mock(UserSessionRepository.class);
    doAnswer(inv -> {
      sessions.put(inv.getArgument(0, UserSession.class).getId(), inv.getArgument(0));
      return null;
    }).when(repository).save(any());
    when(repository.findById(any())).thenAnswer(
        inv -> Optional.ofNullable(sessions.get(inv.getArgument(0, UserSessionId.class))));
    when(repository.findByRefreshTokenHash(anyString())).thenAnswer(inv -> {
      final var hash = inv.getArgument(0, String.class);
      return sessions.values().stream().filter(s -> s.containsRefreshTokenHash(hash)).findFirst();
    });

    final var counter = new AtomicInteger(1);
    final var refreshTokenProvider = mock(RefreshTokenProvider.class);
    when(refreshTokenProvider.hash(anyString())).thenAnswer(
        inv -> "hash-" + inv.getArgument(0, String.class));
    when(refreshTokenProvider.issue(any())).thenAnswer(inv -> {
      final var value = "refresh-" + counter.getAndIncrement();
      return new RefreshTokenProvider.IssuedRefreshToken(value, "hash-" + value,
          inv.getArgument(0, Instant.class).plusSeconds(AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN),
          AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN);
    });

    final var accessTokenIssuer = mock(AccessTokenIssuer.class);
    when(accessTokenIssuer.issueForUser(any())).thenAnswer(
        inv -> new AccessTokenIssuer.IssuedAccessToken(
            "access-" + inv.getArgument(0, PlayerId.class).value(),
            AuthTestFixtures.USER_ACCESS_TOKEN_EXPIRES_IN));

    final var current = UserSession.issue(playerId, refreshTokenProvider.hash("refresh-0"),
        AuthTestFixtures.NOW,
        AuthTestFixtures.NOW.plusSeconds(AuthTestFixtures.REFRESH_TOKEN_EXPIRES_IN));
    repository.save(current);

    final var issuer = new UserSessionIssuer(accessTokenIssuer, refreshTokenProvider, repository,
        AuthTestFixtures.fixedClock());
    final var userQueryRepository = mock(UserQueryRepository.class);
    when(userQueryRepository.findUsernameById(playerId)).thenReturn(Optional.of("juancho"));
    final var handler = new RefreshUserSessionCommandHandler(repository, refreshTokenProvider,
        issuer, userQueryRepository);

    final var refreshed = handler.handle(new RefreshUserSessionCommand("refresh-0"));

    assertThat(refreshed.username()).isEqualTo("juancho");
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
