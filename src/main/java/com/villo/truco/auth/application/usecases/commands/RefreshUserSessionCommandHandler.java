package com.villo.truco.auth.application.usecases.commands;

import com.villo.truco.application.exceptions.UnauthorizedAccessException;
import com.villo.truco.auth.application.commands.RefreshUserSessionCommand;
import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import com.villo.truco.auth.application.ports.in.RefreshUserSessionUseCase;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.application.services.UserSessionIssuer;
import com.villo.truco.auth.domain.model.auth.exceptions.InvalidUserSessionRefreshException;
import com.villo.truco.auth.domain.ports.UserQueryRepository;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import java.util.Objects;

public final class RefreshUserSessionCommandHandler implements RefreshUserSessionUseCase {

  private final UserSessionRepository userSessionRepository;
  private final RefreshTokenProvider refreshTokenProvider;
  private final UserSessionIssuer userSessionIssuer;
  private final UserQueryRepository userQueryRepository;

  public RefreshUserSessionCommandHandler(final UserSessionRepository userSessionRepository,
      final RefreshTokenProvider refreshTokenProvider, final UserSessionIssuer userSessionIssuer,
      final UserQueryRepository userQueryRepository) {

    this.userSessionRepository = Objects.requireNonNull(userSessionRepository);
    this.refreshTokenProvider = Objects.requireNonNull(refreshTokenProvider);
    this.userSessionIssuer = Objects.requireNonNull(userSessionIssuer);
    this.userQueryRepository = Objects.requireNonNull(userQueryRepository);
  }

  @Override
  public UserAuthenticatedSession handle(final RefreshUserSessionCommand command) {

    final var providedHash = this.refreshTokenProvider.hash(command.refreshToken());
    final var userSession = this.userSessionRepository.findByRefreshTokenHash(providedHash)
        .orElseThrow(InvalidUserSessionRefreshException::new);
    final var username = this.userQueryRepository.findUsernameById(userSession.userId())
        .orElseThrow(() -> new UnauthorizedAccessException("Authenticated user not found"));

    try {
      return this.userSessionIssuer.rotate(userSession, username, providedHash);
    } catch (InvalidUserSessionRefreshException ex) {
      this.userSessionRepository.save(userSession);
      throw ex;
    }
  }

}
