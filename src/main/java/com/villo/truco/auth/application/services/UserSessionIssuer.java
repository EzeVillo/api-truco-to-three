package com.villo.truco.auth.application.services;

import com.villo.truco.auth.application.model.UserAuthenticatedSession;
import com.villo.truco.auth.application.ports.out.AccessTokenIssuer;
import com.villo.truco.auth.application.ports.out.RefreshTokenProvider;
import com.villo.truco.auth.domain.model.auth.UserSession;
import com.villo.truco.auth.domain.ports.UserSessionRepository;
import com.villo.truco.domain.shared.valueobjects.PlayerId;
import java.time.Clock;
import java.util.Objects;

public final class UserSessionIssuer {

  private final AccessTokenIssuer accessTokenIssuer;
  private final RefreshTokenProvider refreshTokenProvider;
  private final UserSessionRepository userSessionRepository;
  private final Clock clock;

  public UserSessionIssuer(final AccessTokenIssuer accessTokenIssuer,
      final RefreshTokenProvider refreshTokenProvider,
      final UserSessionRepository userSessionRepository, final Clock clock) {

    this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
    this.refreshTokenProvider = Objects.requireNonNull(refreshTokenProvider);
    this.userSessionRepository = Objects.requireNonNull(userSessionRepository);
    this.clock = Objects.requireNonNull(clock);
  }

  public UserAuthenticatedSession issueFor(final PlayerId userId) {

    final var issuedAt = this.clock.instant();
    final var refreshToken = this.refreshTokenProvider.issue(issuedAt);
    final var userSession = UserSession.issue(userId, refreshToken.hash(), issuedAt,
        refreshToken.expiresAt());
    final var accessToken = this.accessTokenIssuer.issueForUser(userId);

    this.userSessionRepository.save(userSession);

    return new UserAuthenticatedSession(userId, accessToken.value(), accessToken.expiresIn(),
        refreshToken.value(), refreshToken.expiresIn());
  }

  public UserAuthenticatedSession rotate(final UserSession userSession,
      final String providedRefreshTokenHash) {

    final var issuedAt = this.clock.instant();
    final var refreshToken = this.refreshTokenProvider.issue(issuedAt);
    userSession.rotate(providedRefreshTokenHash, refreshToken.hash(), issuedAt,
        refreshToken.expiresAt());
    this.userSessionRepository.save(userSession);
    final var accessToken = this.accessTokenIssuer.issueForUser(userSession.userId());

    return new UserAuthenticatedSession(userSession.userId(), accessToken.value(),
        accessToken.expiresIn(), refreshToken.value(), refreshToken.expiresIn());
  }

}
