package com.villo.truco.auth.application.usecases.commands;

import com.villo.truco.auth.application.commands.LogoutUserSessionCommand;
import com.villo.truco.auth.application.ports.in.LogoutUserSessionUseCase;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import java.time.Clock;
import java.util.Objects;

public final class LogoutUserSessionCommandHandler implements LogoutUserSessionUseCase {

  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenProvider refreshTokenProvider;
  private final Clock clock;

  public LogoutUserSessionCommandHandler(final UserSessionRepository userSessionRepository,
      final RefreshTokenProvider refreshTokenProvider, final Clock clock) {

    this.userSessionRepository = Objects.requireNonNull(userSessionRepository);
    this.refreshTokenProvider = Objects.requireNonNull(refreshTokenProvider);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Void handle(final LogoutUserSessionCommand command) {

    final var tokenHash = this.refreshTokenProvider.hash(command.refreshToken());
    this.userSessionRepository.findByRefreshTokenHash(tokenHash).ifPresent(userSession -> {
      userSession.revoke(this.clock.instant());
      this.userSessionRepository.save(userSession);
    });
    return null;
  }

}
