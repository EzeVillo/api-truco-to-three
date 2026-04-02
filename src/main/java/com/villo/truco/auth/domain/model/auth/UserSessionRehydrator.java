package com.villo.truco.auth.domain.model.auth;

import java.util.Objects;

public final class UserSessionRehydrator {

  private UserSessionRehydrator() {

  }

  public static UserSession rehydrate(final UserSessionSnapshot snapshot) {

    Objects.requireNonNull(snapshot);
    final var refreshTokens = snapshot.refreshTokens().stream().map(
        tokenSnapshot -> RefreshToken.rehydrate(tokenSnapshot.id(), tokenSnapshot.userId(),
            tokenSnapshot.tokenHash(), tokenSnapshot.expiresAt(), tokenSnapshot.createdAt(),
            tokenSnapshot.revokedAt(), tokenSnapshot.rotatedAt(),
            tokenSnapshot.replacedByTokenId())).toList();
    return UserSession.rehydrate(snapshot.id(), snapshot.userId(), refreshTokens);
  }

}
