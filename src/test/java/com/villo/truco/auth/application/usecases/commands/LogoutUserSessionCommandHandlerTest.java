package com.villo.truco.auth.application.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.villo.truco.auth.application.commands.LogoutUserSessionCommand;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.model.auth.valueobjects.UserSessionId;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LogoutUserSessionCommandHandler")
class LogoutUserSessionCommandHandlerTest {

  @Test
  @DisplayName("revoca solo la sesion del refresh token enviado")
  void revokesOnlyMatchingSession() {

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

    final var provider = mock(RefreshTokenProvider.class);
    when(provider.hash(anyString())).thenAnswer(inv -> "hash-" + inv.getArgument(0, String.class));

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
